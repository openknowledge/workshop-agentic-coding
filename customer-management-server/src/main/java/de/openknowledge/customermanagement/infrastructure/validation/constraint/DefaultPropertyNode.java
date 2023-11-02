/*
 * Copyright (C) open knowledge GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.openknowledge.customermanagement.infrastructure.validation.constraint;

import jakarta.validation.ElementKind;
import jakarta.validation.Path.PropertyNode;

public class DefaultPropertyNode extends AbstractNode implements PropertyNode {

    private Class<?> containerClass;
    private Integer typeArgumentIndex;

    public DefaultPropertyNode(String name) {
        super(name);
    }

    public DefaultPropertyNode(String name, Integer index) {
        super(name, index);
    }

    public DefaultPropertyNode(String name, Object key) {
        super(name, key);
    }

    public DefaultPropertyNode(String name, Class<?> containerClass, Integer typeArgumentIndex) {
        super(name);
        this.containerClass = containerClass;
        this.typeArgumentIndex = typeArgumentIndex;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.PROPERTY;
    }

    @Override
    public Class<?> getContainerClass() {
        return containerClass;
    }

    @Override
    public Integer getTypeArgumentIndex() {
        return typeArgumentIndex;
    }
}
