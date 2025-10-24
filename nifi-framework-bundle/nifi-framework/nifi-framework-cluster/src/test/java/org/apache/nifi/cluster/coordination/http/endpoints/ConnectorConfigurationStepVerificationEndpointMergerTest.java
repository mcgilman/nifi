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
package org.apache.nifi.cluster.coordination.http.endpoints;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorConfigurationStepVerificationEndpointMergerTest {

    @Test
    void testCanHandle() throws Exception {
        final ConnectorConfigurationStepVerificationEndpointMerger merger = new ConnectorConfigurationStepVerificationEndpointMerger();

        final URI validUri = new URI("http://localhost:8080/nifi-api/connectors/12345678-1234-1234-1234-123456789012/configuration-steps/step1/verify-config");
        assertTrue(merger.canHandle(validUri, "POST"));

        final URI invalidUriGet = new URI("http://localhost:8080/nifi-api/connectors/12345678-1234-1234-1234-123456789012/configuration-steps/step1/verify-config");
        assertFalse(merger.canHandle(invalidUriGet, "GET"));

        final URI invalidUriPath = new URI("http://localhost:8080/nifi-api/connectors/12345678-1234-1234-1234-123456789012/configuration-steps");
        assertFalse(merger.canHandle(invalidUriPath, "POST"));

        final URI invalidUriOtherResource = new URI("http://localhost:8080/nifi-api/processors/12345678-1234-1234-1234-123456789012/verify-config");
        assertFalse(merger.canHandle(invalidUriOtherResource, "POST"));
    }
}

