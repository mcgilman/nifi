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
package org.apache.nifi.web.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlType;

/**
 * A reference to a connector property value, which includes the value and its type.
 */
@XmlType(name = "connectorValueReference")
public class ConnectorValueReferenceDTO {

    private String value;
    private String valueType;

    /**
     * @return the property value
     */
    @Schema(description = "The property value.")
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * @return the type of value (STRING_LITERAL, ASSET_REFERENCE, or SECRET_REFERENCE)
     */
    @Schema(description = "The type of value (STRING_LITERAL, ASSET_REFERENCE, or SECRET_REFERENCE).")
    public String getValueType() {
        return valueType;
    }

    public void setValueType(final String valueType) {
        this.valueType = valueType;
    }
}

