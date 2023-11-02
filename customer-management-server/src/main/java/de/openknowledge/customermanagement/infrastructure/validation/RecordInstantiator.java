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

import static java.util.Arrays.stream;

import de.openknowledge.customermanagement.infrastructure.validation.constraint.ConstraintViolationContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.ValueInstantiator.Delegating;
import tools.jackson.databind.deser.bean.PropertyValueBuffer;

public class RecordInstantiator extends Delegating {

    private Constructor<?> constructor;
    private Validator validator;

    public RecordInstantiator(
            ValueInstantiator delegate, Class<? extends Record> recordType, Validator validator) {
        super(delegate);
        this.constructor = getCanonicalConstructor(recordType);
        this.validator = validator;
    }

    @Override
    public Object createFromObjectWith(
            DeserializationContext context,
            SettableBeanProperty[] properties,
            PropertyValueBuffer buffer)
            throws JacksonException {
        return isValid(getViolationContext(context), buffer.getParameters(context, properties))
                ? super.createFromObjectWith(context, properties, buffer)
                : null;
    }

    @Override
    public Object createFromString(DeserializationContext context, String value)
            throws JacksonException {
        return isValid(getParentViolationContext(context), value)
                ? super.createFromString(context, value)
                : null;
    }

    @Override
    public Object createFromInt(DeserializationContext context, int value) throws JacksonException {
        return isValid(getParentViolationContext(context), value)
                ? super.createFromInt(context, value)
                : null;
    }

    @Override
    public Object createFromLong(DeserializationContext context, long value)
            throws JacksonException {
        return isValid(getParentViolationContext(context), value)
                ? super.createFromLong(context, value)
                : null;
    }

    @Override
    public Object createFromBigInteger(DeserializationContext context, BigInteger value)
            throws JacksonException {
        return isValid(getParentViolationContext(context), value)
                ? super.createFromBigInteger(context, value)
                : null;
    }

    @Override
    public Object createFromDouble(DeserializationContext context, double value)
            throws JacksonException {
        return isValid(getParentViolationContext(context), value)
                ? super.createFromDouble(context, value)
                : null;
    }

    @Override
    public Object createFromBigDecimal(DeserializationContext context, BigDecimal value)
            throws JacksonException {
        return isValid(getParentViolationContext(context), value)
                ? super.createFromBigDecimal(context, value)
                : null;
    }

    @Override
    public Object createFromBoolean(DeserializationContext context, boolean value)
            throws JacksonException {
        return isValid(getParentViolationContext(context), value)
                ? super.createFromBoolean(context, value)
                : null;
    }

    private ConstraintViolationContext getViolationContext(DeserializationContext context) {
        return (ConstraintViolationContext) context.getAttribute(ConstraintViolationContext.class);
    }

    private ConstraintViolationContext getParentViolationContext(DeserializationContext context) {
        return getViolationContext(context).useParentPath();
    }

    private boolean isValid(ConstraintViolationContext context, Object... parameters)
            throws JacksonException {
        var newViolations = validateConstructor(constructor, parameters);
        context.addAll(newViolations);
        return newViolations.isEmpty();
    }

    private Set<ConstraintViolation<?>> validateConstructor(
            Constructor<?> constructorToValidate, Object... parameters) {
        ExecutableValidator executableValidator = validator.forExecutables();
        var violations =
                executableValidator.validateConstructorParameters(
                        constructorToValidate, parameters);
        return (Set<ConstraintViolation<?>>) violations;
    }

    public static <T extends Record> Constructor<T> getCanonicalConstructor(Class<T> recordType) {
        try {
            return recordType.getConstructor(
                    stream(recordType.getRecordComponents())
                            .map(RecordComponent::getType)
                            .toArray(Class[]::new));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
