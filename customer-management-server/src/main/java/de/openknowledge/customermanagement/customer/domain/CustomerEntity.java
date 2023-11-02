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

import static jakarta.persistence.GenerationType.SEQUENCE;
import static java.util.Objects.requireNonNull;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "Customer")
@Table(name = "TAB_CUSTOMER")
public class CustomerEntity {

    @Id
    @Column(name = "C_ID")
    @GeneratedValue(strategy = SEQUENCE, generator = "SEQ_CUSTOMER")
    @SequenceGenerator(
            name = "SEQ_CUSTOMER",
            sequenceName = "SEQ_CUSTOMER_C_ID",
            allocationSize = 1)
    private Long id;

    @Embedded
    @AttributeOverride(
            name = "name.firstName.firstName",
            column =
                    @Column(name = "C_FIRST_NAME", length = FirstName.MAX_LENGTH, nullable = false))
    @AttributeOverride(
            name = "name.lastName.lastName",
            column = @Column(name = "C_LAST_NAME", length = LastName.MAX_LENGTH, nullable = false))
    @AttributeOverride(
            name = "email.email",
            column = @Column(name = "C_EMAIL", length = Email.MAX_LENGTH, nullable = false))
    @AttributeOverride(
            name = "phoneNumber.phoneNumber",
            column = @Column(name = "C_PHONE_NUMBER", length = PhoneNumber.MAX_LENGTH))
    @AttributeOverride(name = "birthDate.birthDate", column = @Column(name = "C_BIRTH_DATE"))
    @AttributeOverride(
            name = "externalUserId.externalUserId",
            column = @Column(name = "C_EXTERNAL_USER_ID"))
    private Customer customer;

    public CustomerEntity(Customer customer) {
        this.customer = requireNonNull(customer);
    }

    protected CustomerEntity() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void update(Customer newCustomer) {
        customer = requireNonNull(newCustomer);
    }
}
