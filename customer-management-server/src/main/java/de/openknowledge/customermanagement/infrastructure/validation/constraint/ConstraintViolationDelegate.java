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

import static java.util.Objects.requireNonNull;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;

public class ConstraintViolationDelegate<T> implements ConstraintViolation<T> {

    private ConstraintViolation<T> violation;

    public ConstraintViolationDelegate(ConstraintViolation<T> violation) {
        this.violation = requireNonNull(violation);
    }

    public String getMessage() {
        return violation.getMessage();
    }

    public String getMessageTemplate() {
        return violation.getMessageTemplate();
    }

    public T getRootBean() {
        return violation.getRootBean();
    }

    public Class<T> getRootBeanClass() {
        return violation.getRootBeanClass();
    }

    public Object getLeafBean() {
        return violation.getLeafBean();
    }

    public Object[] getExecutableParameters() {
        return violation.getExecutableParameters();
    }

    public Object getExecutableReturnValue() {
        return violation.getExecutableReturnValue();
    }

    public Path getPropertyPath() {
        return violation.getPropertyPath();
    }

    public Object getInvalidValue() {
        return violation.getInvalidValue();
    }

    public ConstraintDescriptor<?> getConstraintDescriptor() {
        return violation.getConstraintDescriptor();
    }

    public <U> U unwrap(Class<U> type) {
        return violation.unwrap(type);
    }

    @Override
    public String toString() {
        return violation.toString();
    }
}
