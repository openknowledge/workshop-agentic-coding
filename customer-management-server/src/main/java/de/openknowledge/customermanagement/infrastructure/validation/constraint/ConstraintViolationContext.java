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

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Path.Node;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class ConstraintViolationContext {

    private Set<ConstraintViolation<?>> violations;
    private Path path;
    private boolean useParentPath;

    public ConstraintViolationContext() {
        this(new HashSet<>(), new DefaultPath(), false);
    }

    public ConstraintViolationContext(String property) {
        this(new HashSet<>(), new DefaultPath(new DefaultPropertyNode(property)), false);
    }

    private ConstraintViolationContext(
            Set<ConstraintViolation<?>> violations, Path path, boolean useParentPath) {
        this.violations = requireNonNull(violations);
        this.path = requireNonNull(path);
        this.useParentPath = useParentPath;
    }

    public void addAll(Set<ConstraintViolation<?>> newViolations) {
        newViolations.stream()
                .map(v -> withUpdatedPath(v))
                .filter(v -> !isInherited(v))
                .forEach(violations::add);
    }

    public Set<ConstraintViolation<?>> getViolations() {
        return unmodifiableSet(violations);
    }

    public Path getPath() {
        return path;
    }

    public ConstraintViolationContext useParentPath() {
        return new ConstraintViolationContext(violations, path, true);
    }

    public ConstraintViolationContext withPath(Path newPath) {
        return new ConstraintViolationContext(violations, newPath, false);
    }

    public ConstraintViolationContext forSubpath(String property) {
        return forSubpath(new DefaultPropertyNode(property));
    }

    public ConstraintViolationContext forSubpath(Node node) {
        return new ConstraintViolationContext(violations, new DefaultPath(path, node), false);
    }

    private Path subpath(Node node) {
        return new DefaultPath(path, node);
    }

    private boolean isInherited(ConstraintViolation<?> violation) {
        if (!violation
                .getMessageTemplate()
                .equals("{jakarta.validation.constraints.NotNull.message}")) {
            return false;
        }
        String prefix = violation.getPropertyPath().toString();
        return violations.stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .anyMatch(p -> p.equals(prefix) || p.startsWith(prefix + '.'));
    }

    private ConstraintViolation<?> withUpdatedPath(ConstraintViolation<?> violation) {
        Path subpath = path;
        if (!useParentPath) {
            subpath = getLastNode(violation.getPropertyPath()).map(this::subpath).orElse(path);
        }
        return new FixedPathConstraintViolation<>(subpath, violation);
    }

    private Optional<Node> getLastNode(Path aPath) {
        Iterator<Node> nodeIterator = aPath.iterator();
        Node lastNode = null;
        while (nodeIterator.hasNext()) {
            lastNode = nodeIterator.next();
        }
        return ofNullable(lastNode);
    }
}
