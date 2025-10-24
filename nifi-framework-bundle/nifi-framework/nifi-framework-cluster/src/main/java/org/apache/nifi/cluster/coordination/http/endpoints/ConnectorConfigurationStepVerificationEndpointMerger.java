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

import org.apache.nifi.cluster.manager.NodeResponse;
import org.apache.nifi.cluster.protocol.NodeIdentifier;
import org.apache.nifi.web.api.dto.ConfigVerificationResultDTO;
import org.apache.nifi.web.api.entity.ConfigurationStepVerificationResultsEntity;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ConnectorConfigurationStepVerificationEndpointMerger extends AbstractSingleEntityEndpoint<ConfigurationStepVerificationResultsEntity> {
    public static final Pattern VERIFY_CONNECTOR_CONFIG_STEP_URI_PATTERN = Pattern.compile("/nifi-api/connectors/[a-f0-9\\-]{36}/configuration-steps/[^/]+/verify-config");

    @Override
    protected Class<ConfigurationStepVerificationResultsEntity> getEntityClass() {
        return ConfigurationStepVerificationResultsEntity.class;
    }

    @Override
    public boolean canHandle(final URI uri, final String method) {
        return "POST".equalsIgnoreCase(method) && VERIFY_CONNECTOR_CONFIG_STEP_URI_PATTERN.matcher(uri.getPath()).matches();
    }

    @Override
    protected void mergeResponses(final ConfigurationStepVerificationResultsEntity clientEntity, final Map<NodeIdentifier, ConfigurationStepVerificationResultsEntity> entityMap,
                                  final Set<NodeResponse> successfulResponses, final Set<NodeResponse> problematicResponses) {

        final List<ConfigVerificationResultDTO> results = clientEntity.getResults();

        // If the result hasn't been set, return immediately
        if (results == null) {
            return;
        }

        // Aggregate the Config Verification Results across all nodes into a single List
        final ConfigVerificationResultMerger resultMerger = new ConfigVerificationResultMerger();
        for (final Map.Entry<NodeIdentifier, ConfigurationStepVerificationResultsEntity> entry : entityMap.entrySet()) {
            final NodeIdentifier nodeId = entry.getKey();
            final ConfigurationStepVerificationResultsEntity entity = entry.getValue();

            final List<ConfigVerificationResultDTO> nodeResults = entity.getResults();
            resultMerger.addNodeResults(nodeId, nodeResults);
        }

        final List<ConfigVerificationResultDTO> aggregateResults = resultMerger.computeAggregateResults();

        clientEntity.setResults(aggregateResults);
    }

}

