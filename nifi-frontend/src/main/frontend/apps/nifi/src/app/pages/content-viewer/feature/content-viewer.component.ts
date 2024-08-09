/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, OnDestroy, OnInit, SecurityContext } from '@angular/core';
import { Store } from '@ngrx/store';
import { NiFiState } from '../../../state';
import { loadContentViewerOptions, resetContentViewerOptions } from '../state/viewer-options/viewer-options.actions';
import { FormBuilder, FormGroup } from '@angular/forms';
import { selectUiProvidedViewerOptions, selectViewerOptions } from '../state/viewer-options/viewer-options.selectors';
import { ContentViewer, HEX_VIEWER_URL, IMAGE_VIEWER_URL, SupportedMimeTypes } from '../state/viewer-options';
import { SelectGroup, SelectOption, selectQueryParams, TextTip } from '@nifi/shared';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { concatLatestFrom } from '@ngrx/operators';

@Component({
    selector: 'content-viewer',
    templateUrl: './content-viewer.component.html',
    styleUrls: ['./content-viewer.component.scss']
})
export class ContentViewerComponent implements OnInit, OnDestroy {
    viewerForm: FormGroup;
    viewAsOptions: SelectGroup[] = [];

    frameSource: SafeResourceUrl | null = null;
    currentProvidedUi: string | null = null;

    private supportedMimeTypeId = 0;
    private supportedMimeTypeLookup: Map<number, SupportedMimeTypes> = new Map<number, SupportedMimeTypes>();
    private supportedMimeTypeContentViewerLookup: Map<number, ContentViewer> = new Map<number, ContentViewer>();
    private uiProvidedSupportedMimeTypeIds: Set<number> = new Set<number>();

    private defaultSupportedMimeTypeId: number | null = null;

    ref: string | undefined;
    private mimeType: string | undefined;
    private filename: string | undefined;

    constructor(
        private store: Store<NiFiState>,
        private formBuilder: FormBuilder,
        private domSanitizer: DomSanitizer
    ) {
        this.viewerForm = this.formBuilder.group({ viewAs: '' });
        this.viewerForm.controls['viewAs'].valueChanges.pipe(takeUntilDestroyed()).subscribe((value) => {
            const viewer = this.supportedMimeTypeContentViewerLookup.get(Number(value));
            if (viewer) {
                if (this.uiProvidedSupportedMimeTypeIds.has(Number(value))) {
                    this.frameSource = null;
                    this.currentProvidedUi = viewer.uri;
                } else {
                    this.frameSource = this.getFrameSource(viewer.uri);
                    this.currentProvidedUi = null;
                }
            }
        });

        this.store
            .select(selectViewerOptions)
            .pipe(
                concatLatestFrom(() => this.store.select(selectUiProvidedViewerOptions)),
                takeUntilDestroyed()
            )
            .subscribe(([discoveredViewerOptions, uiProvidedViewerOptions]) => {
                this.supportedMimeTypeLookup.clear();
                this.supportedMimeTypeContentViewerLookup.clear();
                this.uiProvidedSupportedMimeTypeIds.clear();

                // maps a given content (by display name) to the supported mime type id
                // which can be used to look up the corresponding content viewer
                const supportedMimeTypeMapping = new Map<string, number[]>();

                // process all discovered viewer options
                discoveredViewerOptions.forEach((contentViewer) => {
                    contentViewer.supportedMimeTypes.forEach((supportedMimeType) => {
                        const supportedMimeTypeId = this.supportedMimeTypeId++;

                        if (!supportedMimeTypeMapping.has(supportedMimeType.displayName)) {
                            supportedMimeTypeMapping.set(supportedMimeType.displayName, []);
                        }
                        supportedMimeTypeMapping.get(supportedMimeType.displayName)?.push(supportedMimeTypeId);

                        this.supportedMimeTypeLookup.set(supportedMimeTypeId, supportedMimeType);
                        this.supportedMimeTypeContentViewerLookup.set(supportedMimeTypeId, contentViewer);
                    });
                });

                // process all ui provided options
                uiProvidedViewerOptions.forEach((contentViewer) => {
                    contentViewer.supportedMimeTypes.forEach((supportedMimeType) => {
                        const supportedMimeTypeId = this.supportedMimeTypeId++;

                        if (contentViewer.uri === HEX_VIEWER_URL) {
                            this.defaultSupportedMimeTypeId = supportedMimeTypeId;
                        }

                        if (!supportedMimeTypeMapping.has(supportedMimeType.displayName)) {
                            supportedMimeTypeMapping.set(supportedMimeType.displayName, []);
                        }
                        supportedMimeTypeMapping.get(supportedMimeType.displayName)?.push(supportedMimeTypeId);

                        this.uiProvidedSupportedMimeTypeIds.add(supportedMimeTypeId);
                        this.supportedMimeTypeLookup.set(supportedMimeTypeId, supportedMimeType);
                        this.supportedMimeTypeContentViewerLookup.set(supportedMimeTypeId, contentViewer);
                    });
                });

                const newViewAsOptions: SelectGroup[] = [];
                supportedMimeTypeMapping.forEach((contentViewers, displayName) => {
                    const options: SelectOption[] = [];
                    contentViewers.forEach((contentViewerId) => {
                        const contentViewer = this.supportedMimeTypeContentViewerLookup.get(contentViewerId);
                        if (contentViewer) {
                            const option: SelectOption = {
                                text: contentViewer.displayName,
                                value: String(contentViewerId)
                            };
                            options.push(option);
                        }
                    });
                    const groupOption: SelectGroup = {
                        text: displayName,
                        options
                    };
                    newViewAsOptions.push(groupOption);
                });

                this.viewAsOptions = newViewAsOptions;

                this.handleDefaultSelection();
            });

        this.store
            .select(selectQueryParams)
            .pipe(takeUntilDestroyed())
            .subscribe((queryParams) => {
                this.ref = queryParams['ref'];
                this.mimeType = queryParams['mimeType'];
                this.filename = queryParams['filename'];

                this.handleDefaultSelection();
            });
    }

    ngOnInit(): void {
        this.store.dispatch(loadContentViewerOptions());
    }

    private handleDefaultSelection(): void {
        console.log('handle default selection');

        if (this.viewAsOptions.length === 0) {
            this.viewerForm.get('viewAs')?.setValue('null');
        } else if (this.mimeType) {
            const compatibleViewerOption = this.getCompatibleViewer(this.mimeType);
            if (Number.isNaN(compatibleViewerOption)) {
                if (!Number.isNaN(this.defaultSupportedMimeTypeId)) {
                    this.viewerForm.get('viewAs')?.setValue(String(this.defaultSupportedMimeTypeId));
                }
            } else {
                this.viewerForm.get('viewAs')?.setValue(String(compatibleViewerOption));
            }
        } else if (!Number.isNaN(this.defaultSupportedMimeTypeId)) {
            this.viewerForm.get('viewAs')?.setValue(String(this.defaultSupportedMimeTypeId));
        }
    }

    private getCompatibleViewer(mimeType: string): number | null {
        for (let group of this.viewAsOptions) {
            for (let option of group.options) {
                const supportedMimeTypeId: number = Number(option.value);
                if (!Number.isNaN(supportedMimeTypeId)) {
                    const supportedMimeType = this.supportedMimeTypeLookup.get(supportedMimeTypeId);
                    if (supportedMimeType) {
                        if (supportedMimeType.mimeTypes.includes(mimeType)) {
                            return supportedMimeTypeId;
                        }
                    }
                }
            }
        }

        return null;
    }

    private getFrameSource(url: string): SafeResourceUrl | null {
        if (this.ref) {
            const queryParams = new URLSearchParams();
            queryParams.set('ref', this.ref);
            queryParams.set('mode', 'Formatted');
            const urlWithParams = `${url}?${queryParams.toString()}`;

            const sanitizedUrl = this.domSanitizer.sanitize(SecurityContext.URL, urlWithParams);

            if (sanitizedUrl) {
                return this.domSanitizer.bypassSecurityTrustResourceUrl(sanitizedUrl);
            }
        }

        return null;
    }

    ngOnDestroy(): void {
        this.store.dispatch(resetContentViewerOptions());
    }

    protected readonly HEX_VIEWER_URL = HEX_VIEWER_URL;
    protected readonly IMAGE_VIEWER_URL = IMAGE_VIEWER_URL;
}
