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
package de.openknowledge.customermanagement.infrastructure.validation;

import de.openknowledge.customermanagement.infrastructure.validation.constraint.ConstraintViolationContext;
import de.openknowledge.customermanagement.infrastructure.validation.constraint.DefaultPath;
import de.openknowledge.customermanagement.infrastructure.validation.constraint.DefaultPropertyNode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Path.Node;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;

public class ValidatingDeserializer extends DelegatingDeserializer {

    public ValidatingDeserializer(ValueDeserializer<?> delegate) {
        super(delegate);
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
        return new ValidatingDeserializer(newDelegatee);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        ConstraintViolationContext current =
                (ConstraintViolationContext) context.getAttribute(ConstraintViolationContext.class);
        boolean root = current == null;
        if (root) {
            current = new ConstraintViolationContext();
        }
        ConstraintViolationContext previous = current;
        ConstraintViolationContext scoped = current.withPath(buildPath(parser.streamReadContext()));
        context.setAttribute(ConstraintViolationContext.class, scoped);
        try {
            Object value = super.deserialize(parser, context);
            if (root && !previous.getViolations().isEmpty()) {
                throw new ConstraintViolationException(previous.getViolations());
            }
            return value;
        } finally {
            context.setAttribute(ConstraintViolationContext.class, root ? null : previous);
        }
    }

    public static Path buildPath(TokenStreamContext current) {
        List<TokenStreamContext> contexts = new ArrayList<>();
        while (current != null) {
            contexts.add(0, current);
            current = current.getParent();
        }
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < contexts.size(); i++) {
            TokenStreamContext c = contexts.get(i);
            if (!c.inObject() || c.currentName() == null) {
                continue;
            }
            Integer index = null;
            if (i + 1 < contexts.size() && contexts.get(i + 1).inArray()) {
                index = contexts.get(i + 1).getCurrentIndex();
            }
            if (index != null) {
                nodes.add(new DefaultPropertyNode(c.currentName(), index));
            } else {
                nodes.add(new DefaultPropertyNode(c.currentName()));
            }
        }
        return new DefaultPath(nodes);
    }
}
