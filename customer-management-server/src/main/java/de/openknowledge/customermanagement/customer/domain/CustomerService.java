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

import de.openknowledge.customermanagement.todo.integration.DisplayName;
import de.openknowledge.customermanagement.todo.integration.ExternalUserId;
import de.openknowledge.customermanagement.todo.integration.Title;
import de.openknowledge.customermanagement.todo.integration.Todo;
import de.openknowledge.customermanagement.todo.integration.TodoRepository;
import de.openknowledge.customermanagement.todo.integration.TodoUser;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CustomerService {

    private static final Title WELCOME_TODO_TITLE = new Title("Send welcome email");

    @Autowired private CustomerRepository repository;

    @Autowired private TodoRepository todoRepository;

    public List<CustomerEntity> findAllCustomers() {
        return repository.findAll();
    }

    public Optional<CustomerEntity> findCustomer(Long id) {
        return repository.findById(id);
    }

    public CustomerEntity registerCustomer(Customer customer) {
        repository
                .findByCustomerEmail(customer.email())
                .ifPresent(
                        existing -> {
                            throw new IllegalArgumentException("Email already in use");
                        });
        TodoUser externalUser = lookupOrCreateExternalUser(customer);
        todoRepository.createTodo(new Todo(externalUser.id(), WELCOME_TODO_TITLE, false));
        Customer updated = customer.withExternalUserId(externalUser.id());
        return repository.save(new CustomerEntity(updated));
    }

    public void updateCustomer(Long id, Customer customer) {
        repository
                .findByCustomerEmail(customer.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(
                        conflicting -> {
                            throw new IllegalArgumentException("Email already in use");
                        });
        repository
                .findById(id)
                .ifPresentOrElse(
                        entity -> {
                            ExternalUserId preserved = entity.getCustomer().externalUserId();
                            entity.update(customer.withExternalUserId(preserved));
                        },
                        () -> {
                            throw new EntityNotFoundException();
                        });
    }

    public void deleteCustomer(Long id) {
        repository
                .findById(id)
                .ifPresentOrElse(
                        repository::delete,
                        () -> {
                            throw new EntityNotFoundException();
                        });
    }

    private TodoUser lookupOrCreateExternalUser(Customer customer) {
        return todoRepository
                .findUserByEmail(customer.email())
                .orElseGet(
                        () ->
                                todoRepository.createUser(
                                        new TodoUser(
                                                null,
                                                buildDisplayName(customer),
                                                customer.email())));
    }

    private DisplayName buildDisplayName(Customer customer) {
        Name name = customer.name();
        return new DisplayName(name.firstName().firstName() + " " + name.lastName().lastName());
    }
}
