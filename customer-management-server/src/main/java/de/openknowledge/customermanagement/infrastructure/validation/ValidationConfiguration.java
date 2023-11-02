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

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.ValueInstantiators;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class ValidationConfiguration {
    @Bean
    JacksonModule validationModule(Validator validator) {
        return new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                context.addValueInstantiators(
                        new ValueInstantiators.Base() {
                            @Override
                            public ValueInstantiator modifyValueInstantiator(
                                    DeserializationConfig config,
                                    BeanDescription.Supplier beanDescription,
                                    ValueInstantiator defaultInstantiator) {
                                Class<?> beanClass = beanDescription.getBeanClass();
                                if (!beanClass.isRecord()) {
                                    return defaultInstantiator;
                                }
                                return new RecordInstantiator(
                                        defaultInstantiator,
                                        (Class<? extends Record>) beanClass,
                                        validator);
                            }
                        });
                context.addDeserializerModifier(
                        new ValueDeserializerModifier() {

                            @Override
                            public ValueDeserializer<?> modifyDeserializer(
                                    DeserializationConfig config,
                                    BeanDescription.Supplier beanDescription,
                                    ValueDeserializer<?> deserializer) {

                                return new ValidatingDeserializer(deserializer);
                            }
                        });
            }
        };
    }
}
