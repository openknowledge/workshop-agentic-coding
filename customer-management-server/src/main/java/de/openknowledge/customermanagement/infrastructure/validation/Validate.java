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

import static de.openknowledge.customermanagement.infrastructure.validation.RecordInstantiator.getCanonicalConstructor;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.joining;

import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hibernate.validator.internal.engine.messageinterpolation.DefaultLocaleResolver;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class Validate implements ApplicationContextAware {

    private static Validator validator;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        validator = applicationContext.getBean(Validator.class);
    }

    public static void validate(Class<? extends Record> type, Object... parameters) {
        var violations =
                getValidator()
                        .forExecutables()
                        .validateConstructorParameters(getCanonicalConstructor(type), parameters);
        if (!violations.isEmpty()) {
            List<String> messages = new ArrayList<>();
            violations.forEach(
                    violation ->
                            messages.add(
                                    violation.getPropertyPath() + " " + violation.getMessage()));
            sort(messages); // to create the same error message for the same constraints
            throw new IllegalArgumentException(messages.stream().collect(joining(", ")));
        }
    }

    private static Validator getValidator() {
        if (validator == null) {
            MessageInterpolator messageInterpolator =
                    new ResourceBundleMessageInterpolator(
                            Set.of(Locale.ENGLISH),
                            Locale.ENGLISH,
                            new DefaultLocaleResolver(),
                            false);
            validator =
                    Validation.byDefaultProvider()
                            .configure()
                            .messageInterpolator(messageInterpolator)
                            .buildValidatorFactory()
                            .getValidator();
        }
        return validator;
    }
}
