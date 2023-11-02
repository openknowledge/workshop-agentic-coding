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

import jakarta.validation.Path.Node;

public abstract class AbstractNode implements Node {

    private String name;
    private Integer index;
    private Object key;

    protected AbstractNode(String name) {
        this.name = name;
    }

    protected AbstractNode(String name, Integer index) {
        this.name = name;
        this.index = index;
    }

    protected AbstractNode(String name, Object key) {
        this.name = name;
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isInIterable() {
        return getIndex() != null || getKey() != null;
    }

    @Override
    public Integer getIndex() {
        return index;
    }

    @Override
    public Object getKey() {
        return key;
    }

    @Override
    public <T extends Node> T as(Class<T> nodeType) {
        return nodeType.cast(this);
    }
}
