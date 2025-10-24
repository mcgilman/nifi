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
package org.apache.nifi.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.apache.nifi.authorization.AccessDeniedException;
import org.apache.nifi.authorization.AuthorizeAccess;
import org.apache.nifi.authorization.Authorizer;
import org.apache.nifi.controller.ScheduledState;
import org.apache.nifi.util.NiFiProperties;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.Revision;
import org.apache.nifi.web.api.dto.ConnectorDTO;
import org.apache.nifi.web.api.dto.RevisionDTO;
import org.apache.nifi.web.api.entity.ConnectorEntity;
import org.apache.nifi.web.api.entity.ConnectorRunStatusEntity;
import org.apache.nifi.web.api.request.ClientIdParameter;
import org.apache.nifi.web.api.request.LongParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestConnectorResource {

    @InjectMocks
    private ConnectorResource connectorResource;

    @Mock
    private NiFiServiceFacade serviceFacade;

    @Mock
    private Authorizer authorizer;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private NiFiProperties properties;

    private static final String CONNECTOR_ID = "test-connector-id";
    private static final String CONNECTOR_NAME = "Test Connector";
    private static final String CONNECTOR_TYPE = "TestConnectorType";

    @BeforeEach
    public void setUp() {
        when(httpServletRequest.getHeader(any())).thenReturn(null);
        when(properties.isNode()).thenReturn(Boolean.FALSE);
        
        connectorResource.setServiceFacade(serviceFacade);
        connectorResource.httpServletRequest = httpServletRequest;
        connectorResource.properties = properties;
    }

    @Test
    public void testGetConnector() {
        final ConnectorEntity connectorEntity = createConnectorEntity();

        when(serviceFacade.getConnector(CONNECTOR_ID)).thenReturn(connectorEntity);

        try (Response response = connectorResource.getConnector(CONNECTOR_ID)) {
            assertEquals(200, response.getStatus());
            assertEquals(connectorEntity, response.getEntity());
        }

        verify(serviceFacade).authorizeAccess(any(AuthorizeAccess.class));
        verify(serviceFacade).getConnector(CONNECTOR_ID);
    }

    @Test
    public void testGetConnectorNotAuthorized() {
        doThrow(AccessDeniedException.class).when(serviceFacade).authorizeAccess(any(AuthorizeAccess.class));

        assertThrows(AccessDeniedException.class, () -> connectorResource.getConnector(CONNECTOR_ID));

        verify(serviceFacade).authorizeAccess(any(AuthorizeAccess.class));
        verify(serviceFacade, never()).getConnector(anyString());
    }

    @Test
    public void testUpdateConnector() {
        final ConnectorEntity requestEntity = createConnectorEntity();
        final ConnectorEntity responseEntity = createConnectorEntity();

        when(serviceFacade.updateConnector(any(Revision.class), any(ConnectorDTO.class))).thenReturn(responseEntity);

        try (Response response = connectorResource.updateConnector(CONNECTOR_ID, requestEntity)) {
            assertEquals(200, response.getStatus());
            assertEquals(responseEntity, response.getEntity());
        }

        verify(serviceFacade).verifyUpdateConnector(any(ConnectorDTO.class));
        verify(serviceFacade).updateConnector(any(Revision.class), any(ConnectorDTO.class));
    }

    @Test
    public void testUpdateConnectorNotAuthorized() {
        final ConnectorEntity requestEntity = createConnectorEntity();

        doThrow(AccessDeniedException.class).when(serviceFacade).authorizeAccess(any(AuthorizeAccess.class));

        assertThrows(AccessDeniedException.class, () -> connectorResource.updateConnector(CONNECTOR_ID, requestEntity));

        verify(serviceFacade, never()).verifyUpdateConnector(any(ConnectorDTO.class));
        verify(serviceFacade, never()).updateConnector(any(Revision.class), any(ConnectorDTO.class));
    }

    @Test
    public void testUpdateConnectorWithMismatchedId() {
        final ConnectorEntity requestEntity = createConnectorEntity();
        requestEntity.getComponent().setId("different-id");

        assertThrows(IllegalArgumentException.class, () -> 
            connectorResource.updateConnector(CONNECTOR_ID, requestEntity));

        verify(serviceFacade, never()).updateConnector(any(Revision.class), any(ConnectorDTO.class));
    }

    @Test
    public void testUpdateConnectorWithNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> 
            connectorResource.updateConnector(CONNECTOR_ID, null));

        verify(serviceFacade, never()).updateConnector(any(Revision.class), any(ConnectorDTO.class));
    }

    @Test
    public void testUpdateConnectorWithNullComponent() {
        final ConnectorEntity requestEntity = new ConnectorEntity();
        requestEntity.setComponent(null);

        assertThrows(IllegalArgumentException.class, () -> 
            connectorResource.updateConnector(CONNECTOR_ID, requestEntity));

        verify(serviceFacade, never()).updateConnector(any(Revision.class), any(ConnectorDTO.class));
    }

    @Test
    public void testUpdateConnectorWithNullRevision() {
        final ConnectorEntity requestEntity = createConnectorEntity();
        requestEntity.setRevision(null);

        assertThrows(IllegalArgumentException.class, () -> 
            connectorResource.updateConnector(CONNECTOR_ID, requestEntity));

        verify(serviceFacade, never()).updateConnector(any(Revision.class), any(ConnectorDTO.class));
    }

    @Test
    public void testDeleteConnector() {
        final ConnectorEntity responseEntity = createConnectorEntity();

        when(serviceFacade.deleteConnector(any(Revision.class), eq(CONNECTOR_ID))).thenReturn(responseEntity);

        try (Response response = connectorResource.deleteConnector(new LongParameter("1"), new ClientIdParameter("client-id"), false, CONNECTOR_ID)) {
            assertEquals(200, response.getStatus());
            assertEquals(responseEntity, response.getEntity());
        }

        verify(serviceFacade).verifyDeleteConnector(CONNECTOR_ID);
        verify(serviceFacade).deleteConnector(any(Revision.class), eq(CONNECTOR_ID));
    }

    @Test
    public void testDeleteConnectorNotAuthorized() {
        doThrow(AccessDeniedException.class).when(serviceFacade).authorizeAccess(any(AuthorizeAccess.class));

        assertThrows(AccessDeniedException.class, () -> 
            connectorResource.deleteConnector(new LongParameter("1"), new ClientIdParameter("client-id"), false, CONNECTOR_ID));

        verify(serviceFacade, never()).verifyDeleteConnector(anyString());
        verify(serviceFacade, never()).deleteConnector(any(Revision.class), anyString());
    }

    @Test
    public void testUpdateRunStatus() {
        final ConnectorRunStatusEntity requestEntity = createConnectorRunStatusEntity();
        final ConnectorEntity responseEntity = createConnectorEntity();

        when(serviceFacade.scheduleConnector(any(Revision.class), eq(CONNECTOR_ID), eq(ScheduledState.RUNNING)))
            .thenReturn(responseEntity);

        try (Response response = connectorResource.updateRunStatus(CONNECTOR_ID, requestEntity)) {
            assertEquals(200, response.getStatus());
            assertEquals(responseEntity, response.getEntity());
        }

        verify(serviceFacade).verifyUpdateConnector(any(ConnectorDTO.class));
        verify(serviceFacade).scheduleConnector(any(Revision.class), eq(CONNECTOR_ID), eq(ScheduledState.RUNNING));
    }

    @Test
    public void testUpdateRunStatusNotAuthorized() {
        final ConnectorRunStatusEntity requestEntity = createConnectorRunStatusEntity();

        doThrow(AccessDeniedException.class).when(serviceFacade).authorizeAccess(any(AuthorizeAccess.class));

        assertThrows(AccessDeniedException.class, () -> 
            connectorResource.updateRunStatus(CONNECTOR_ID, requestEntity));

        verify(serviceFacade, never()).verifyUpdateConnector(any(ConnectorDTO.class));
        verify(serviceFacade, never()).scheduleConnector(any(Revision.class), anyString(), any(ScheduledState.class));
    }

    @Test
    public void testUpdateRunStatusWithNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> 
            connectorResource.updateRunStatus(CONNECTOR_ID, null));

        verify(serviceFacade, never()).scheduleConnector(any(Revision.class), anyString(), any(ScheduledState.class));
    }

    @Test
    public void testUpdateRunStatusWithNullRevision() {
        final ConnectorRunStatusEntity requestEntity = createConnectorRunStatusEntity();
        requestEntity.setRevision(null);

        assertThrows(IllegalArgumentException.class, () -> 
            connectorResource.updateRunStatus(CONNECTOR_ID, requestEntity));

        verify(serviceFacade, never()).scheduleConnector(any(Revision.class), anyString(), any(ScheduledState.class));
    }

    private ConnectorEntity createConnectorEntity() {
        final ConnectorEntity entity = new ConnectorEntity();
        
        final ConnectorDTO dto = new ConnectorDTO();
        dto.setId(CONNECTOR_ID);
        dto.setName(CONNECTOR_NAME);
        dto.setType(CONNECTOR_TYPE);
        dto.setState("STOPPED");
        entity.setComponent(dto);

        final RevisionDTO revision = new RevisionDTO();
        revision.setVersion(1L);
        revision.setClientId("client-id");
        entity.setRevision(revision);

        return entity;
    }

    private ConnectorRunStatusEntity createConnectorRunStatusEntity() {
        final ConnectorRunStatusEntity entity = new ConnectorRunStatusEntity();
        entity.setState("RUNNING");

        final RevisionDTO revision = new RevisionDTO();
        revision.setVersion(1L);
        revision.setClientId("client-id");
        entity.setRevision(revision);

        return entity;
    }
}
