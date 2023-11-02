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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.List.copyOf;

import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class DefaultPath implements Path {

    private List<Node> nodes;

    public DefaultPath() {
        nodes = emptyList();
    }

    public DefaultPath(Node node) {
        nodes = singletonList(node);
    }

    public DefaultPath(Path parent, Node child) {
        this(parent, new DefaultPath(child));
    }

    public DefaultPath(Path parent, Path child) {
        this(append(parent, child));
    }

    public DefaultPath(List<Node> pathNodes) {
        nodes = copyOf(pathNodes);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Node node : nodes) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(node.getName());
            if (node.getIndex() != null) {
                builder.append('[').append(node.getIndex()).append(']');
            } else if (node.getKey() != null) {
                builder.append('[').append(node.getKey()).append(']');
            }
        }
        return builder.toString();
    }

    public Iterator<Node> iterator() {
        return nodes.iterator();
    }

    public void forEach(Consumer<? super Node> action) {
        nodes.forEach(action);
    }

    public Spliterator<Node> spliterator() {
        return nodes.spliterator();
    }

    private static List<Node> append(Path parent, Path child) {
        List<Node> allNodes = new ArrayList<>();
        parent.forEach(allNodes::add);
        child.forEach(allNodes::add);
        return allNodes;
    }
}
