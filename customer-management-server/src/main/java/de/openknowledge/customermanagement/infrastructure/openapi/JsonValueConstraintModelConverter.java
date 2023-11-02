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
package de.openknowledge.customermanagement.infrastructure.openapi;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Iterator;
import org.springframework.stereotype.Component;

/**
 * Transfers Bean Validation constraints from the {@code @JsonValue}-annotated component of a record
 * value object to the generated OpenAPI schema, so Swagger UI shows {@code minLength}, {@code
 * maxLength}, {@code format}, etc. without any extra {@code @Schema} annotations.
 */
@Component
public class JsonValueConstraintModelConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(
            AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) {
            return null;
        }
        Schema<?> schema = chain.next().resolve(type, context, chain);
        if (schema == null) {
            return null;
        }
        Class<?> rawClass = resolveRawClass(type);
        if (rawClass != null) {
            applyJsonValueConstraints(rawClass, schema);
        }
        return schema;
    }

    private Class<?> resolveRawClass(AnnotatedType annotatedType) {
        if (annotatedType == null || annotatedType.getType() == null) {
            return null;
        }
        Type t = annotatedType.getType();
        if (t instanceof Class<?> c) {
            return c;
        }
        if (t instanceof JavaType jt) {
            return jt.getRawClass();
        }
        return null;
    }

    private void applyJsonValueConstraints(Class<?> rawClass, Schema<?> schema) {
        if (isExcludedType(rawClass)) {
            return;
        }
        ConstraintSource source = findJsonValueSource(rawClass);
        if (source == null) {
            return;
        }
        applyConstraints(source, schema);
    }

    private boolean isExcludedType(Class<?> rawClass) {
        return rawClass.isEnum() || rawClass.isArray() || isSystemType(rawClass);
    }

    private boolean isSystemType(Class<?> rawClass) {
        String name = rawClass.getName();
        return name.startsWith("java.")
                || name.startsWith("jakarta.")
                || name.startsWith("org.springframework.");
    }

    private ConstraintSource findJsonValueSource(Class<?> rawClass) {
        if (!rawClass.isRecord()) {
            return null;
        }
        ConstraintSource found = null;
        for (RecordComponent rc : rawClass.getRecordComponents()) {
            Method accessor = rc.getAccessor();
            Field field = getDeclaredFieldQuietly(rawClass, rc.getName());
            boolean hasJsonValue =
                    accessor.isAnnotationPresent(JsonValue.class)
                            || (field != null && field.isAnnotationPresent(JsonValue.class));
            if (hasJsonValue) {
                if (found != null) {
                    return null;
                }
                found = new ConstraintSource(rc, field, accessor);
            }
        }
        return found;
    }

    private void applyConstraints(ConstraintSource source, Schema<?> schema) {
        resetConstraints(schema);
        NotBlank notBlank = findAnnotation(NotBlank.class, source);
        if (notBlank != null) {
            applyNotBlankConstraints(schema);
        }

        NotNull notNull = findAnnotation(NotNull.class, source);
        if (notNull != null) {
            schema.setNullable(false);
        }

        Size size = findAnnotation(Size.class, source);
        if (size != null) {
            applySizeConstraints(size, schema);
        }

        Email email = findAnnotation(Email.class, source);
        if (email != null) {
            applyEmailConstraint(schema);
        }

        Pattern pattern = findAnnotation(Pattern.class, source);
        if (pattern != null) {
            applyPatternConstraint(pattern, schema);
        }

        Min min = findAnnotation(Min.class, source);
        if (min != null) {
            applyMinConstraint(min, schema);
        }

        Max max = findAnnotation(Max.class, source);
        if (max != null) {
            applyMaxConstraint(max, schema);
        }
    }

    private static void resetConstraints(Schema<?> schema) {
        schema.setMinLength(null);
        schema.setMaxLength(null);
        schema.setPattern(null);
    }

    private void applyNotBlankConstraints(Schema<?> schema) {
        if (schema.getMinLength() == null || schema.getMinLength() < 1) {
            schema.setMinLength(1);
        }
        schema.setNullable(false);
    }

    private void applySizeConstraints(Size size, Schema<?> schema) {
        int sizeMin = size.min();
        if (sizeMin > 0 && (schema.getMinLength() == null || schema.getMinLength() < sizeMin)) {
            schema.setMinLength(sizeMin);
        }
        if (size.max() < Integer.MAX_VALUE && schema.getMaxLength() == null) {
            schema.setMaxLength(size.max());
        }
    }

    private void applyEmailConstraint(Schema<?> schema) {
        if (schema.getPattern() == null) {
            schema.setPattern("Email");
        }
    }

    private void applyPatternConstraint(Pattern pattern, Schema<?> schema) {
        if (schema.getPattern() == null) {
            schema.setPattern(pattern.regexp());
        }
    }

    private void applyMinConstraint(Min min, Schema<?> schema) {
        if (schema.getMinimum() == null) {
            schema.setMinimum(BigDecimal.valueOf(min.value()));
        }
    }

    private void applyMaxConstraint(Max max, Schema<?> schema) {
        if (schema.getMaximum() == null) {
            schema.setMaximum(BigDecimal.valueOf(max.value()));
        }
    }

    private <A extends Annotation> A findAnnotation(
            Class<A> annotationType, ConstraintSource source) {
        A annotation = source.accessor().getAnnotation(annotationType);
        if (annotation == null && source.field() != null) {
            annotation = source.field().getAnnotation(annotationType);
        }
        return annotation;
    }

    private Field getDeclaredFieldQuietly(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private record ConstraintSource(RecordComponent component, Field field, Method accessor) {}
}
