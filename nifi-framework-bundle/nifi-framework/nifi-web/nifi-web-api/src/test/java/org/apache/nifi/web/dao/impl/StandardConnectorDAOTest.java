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
package org.apache.nifi.web.dao.impl;

import org.apache.nifi.components.connector.ConnectorNode;
import org.apache.nifi.components.connector.ConnectorRepository;
import org.apache.nifi.components.connector.FlowUpdateException;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.web.NiFiCoreException;
import org.apache.nifi.web.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandardConnectorDAOTest {

    private StandardConnectorDAO connectorDAO;

    @Mock
    private FlowController flowController;

    @Mock
    private ConnectorRepository connectorRepository;

    @Mock
    private ConnectorNode connectorNode;

    private static final String CONNECTOR_ID = "test-connector-id";

    @BeforeEach
    void setUp() {
        connectorDAO = new StandardConnectorDAO();
        connectorDAO.setFlowController(flowController);
        
        when(flowController.getConnectorRepository()).thenReturn(connectorRepository);
    }

    @Test
    void testApplyConnectorUpdate() throws Exception {
        when(connectorRepository.getConnector(CONNECTOR_ID)).thenReturn(connectorNode);

        connectorDAO.applyConnectorUpdate(CONNECTOR_ID);

        verify(connectorRepository).getConnector(CONNECTOR_ID);
        verify(connectorRepository).applyUpdate(connectorNode);
    }

    @Test
    void testApplyConnectorUpdateWithNonExistentConnector() throws Exception {
        when(connectorRepository.getConnector(CONNECTOR_ID)).thenReturn(null);

        final ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            connectorDAO.applyConnectorUpdate(CONNECTOR_ID)
        );

        assertEquals("Could not find Connector with ID " + CONNECTOR_ID, exception.getMessage());
        verify(connectorRepository).getConnector(CONNECTOR_ID);
        verify(connectorRepository, never()).applyUpdate(any(ConnectorNode.class));
    }

    @Test
    void testApplyConnectorUpdateWithFlowUpdateException() throws Exception {
        when(connectorRepository.getConnector(CONNECTOR_ID)).thenReturn(connectorNode);
        doThrow(new FlowUpdateException("Flow update failed")).when(connectorRepository).applyUpdate(connectorNode);

        final NiFiCoreException exception = assertThrows(NiFiCoreException.class, () ->
            connectorDAO.applyConnectorUpdate(CONNECTOR_ID)
        );

        assertEquals("Failed to apply connector update: org.apache.nifi.components.connector.FlowUpdateException: Flow update failed", exception.getMessage());
        verify(connectorRepository).getConnector(CONNECTOR_ID);
        verify(connectorRepository).applyUpdate(connectorNode);
    }

    @Test
    void testApplyConnectorUpdateWithRuntimeException() throws Exception {
        when(connectorRepository.getConnector(CONNECTOR_ID)).thenReturn(connectorNode);
        doThrow(new RuntimeException("Test exception")).when(connectorRepository).applyUpdate(connectorNode);

        final NiFiCoreException exception = assertThrows(NiFiCoreException.class, () ->
            connectorDAO.applyConnectorUpdate(CONNECTOR_ID)
        );

        assertEquals("Failed to apply connector update: java.lang.RuntimeException: Test exception", exception.getMessage());
        verify(connectorRepository).getConnector(CONNECTOR_ID);
        verify(connectorRepository).applyUpdate(connectorNode);
    }

    @Test
    void testApplyConnectorUpdateWithNullException() throws Exception {
        when(connectorRepository.getConnector(CONNECTOR_ID)).thenReturn(connectorNode);
        doThrow(new RuntimeException()).when(connectorRepository).applyUpdate(connectorNode);

        final NiFiCoreException exception = assertThrows(NiFiCoreException.class, () ->
            connectorDAO.applyConnectorUpdate(CONNECTOR_ID)
        );

        assertEquals("Failed to apply connector update: java.lang.RuntimeException", exception.getMessage());
        verify(connectorRepository).getConnector(CONNECTOR_ID);
        verify(connectorRepository).applyUpdate(connectorNode);
    }

    @Test
    void testGetConnectorWithNonExistentId() {
        when(connectorRepository.getConnector(CONNECTOR_ID)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
            connectorDAO.getConnector(CONNECTOR_ID)
        );

        verify(connectorRepository).getConnector(CONNECTOR_ID);
    }

    @Test
    void testGetConnectorSuccess() {
        when(connectorRepository.getConnector(CONNECTOR_ID)).thenReturn(connectorNode);

        final ConnectorNode result = connectorDAO.getConnector(CONNECTOR_ID);

        assertEquals(connectorNode, result);
        verify(connectorRepository).getConnector(CONNECTOR_ID);
    }
}

