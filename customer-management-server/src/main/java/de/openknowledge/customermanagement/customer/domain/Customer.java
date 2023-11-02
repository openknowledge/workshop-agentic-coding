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
package de.openknowledge.customermanagement.customer.domain;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import de.openknowledge.customermanagement.todo.integration.ExternalUserId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Embeddable
public record Customer(
        @Valid @NotNull Name name,
        @Valid @NotNull Email email,
        @Valid PhoneNumber phoneNumber,
        @Valid BirthDate birthDate,
        @ElementCollection(fetch = FetchType.EAGER)
                @CollectionTable(
                        name = "TAB_CUSTOMER_ADDITIONAL_EMAIL",
                        joinColumns =
                                @JoinColumn(
                                        name = "C_CUSTOMER_ID",
                                        nullable = false,
                                        foreignKey =
                                                @ForeignKey(
                                                        name =
                                                                "fk_customer_additional_email_customer")))
                @AttributeOverride(
                        name = "email",
                        column =
                                @Column(
                                        name = "C_EMAIL",
                                        length = Email.MAX_LENGTH,
                                        nullable = false))
                @OrderColumn(name = "C_POSITION")
                List<@Valid Email> additionalEmails,
        @ElementCollection(fetch = FetchType.EAGER)
                @CollectionTable(
                        name = "TAB_CUSTOMER_ADDITIONAL_PHONE_NUMBER",
                        joinColumns =
                                @JoinColumn(
                                        name = "C_CUSTOMER_ID",
                                        nullable = false,
                                        foreignKey =
                                                @ForeignKey(
                                                        name =
                                                                "fk_customer_additional_phone_number_customer")))
                @AttributeOverride(
                        name = "type.name",
                        column =
                                @Column(
                                        name = "C_TYPE",
                                        length = ContactType.MAX_LENGTH,
                                        nullable = false))
                @AttributeOverride(
                        name = "phoneNumber.phoneNumber",
                        column =
                                @Column(
                                        name = "C_PHONE_NUMBER",
                                        length = PhoneNumber.MAX_LENGTH,
                                        nullable = false))
                @OrderColumn(name = "C_POSITION")
                List<@Valid Contact> additionalPhoneNumbers,
        @Valid ExternalUserId externalUserId) {

    public Customer withExternalUserId(ExternalUserId newExternalUserId) {
        return new Customer(
                name,
                email,
                phoneNumber,
                birthDate,
                additionalEmails,
                additionalPhoneNumbers,
                newExternalUserId);
    }

    public List<Email> additionalEmails() {
        return ofNullable(additionalEmails).map(Collections::unmodifiableList).orElse(emptyList());
    }

    public List<Contact> additionalPhoneNumbers() {
        return ofNullable(additionalPhoneNumbers)
                .map(Collections::unmodifiableList)
                .orElse(emptyList());
    }
}
