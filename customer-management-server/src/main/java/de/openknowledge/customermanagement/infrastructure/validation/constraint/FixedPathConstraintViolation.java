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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;

public class FixedPathConstraintViolation<T> extends ConstraintViolationDelegate<T> {

    private final Path propertyPath;

    public FixedPathConstraintViolation(Path path, ConstraintViolation<T> violation) {
        super(violation);
        this.propertyPath = path;
    }

    @Override
    public Path getPropertyPath() {
        return propertyPath;
    }
}
