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
package de.openknowledge.customermanagement.todo.integration;

import static java.util.Optional.ofNullable;

import de.openknowledge.customermanagement.customer.domain.Email;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TodoRepository {

    private static final ParameterizedTypeReference<List<TodoUser>> USER_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private RestClient restClient;

    public TodoRepository(RestClient.Builder builder, TodoProperties properties) {
        this.restClient = builder.clone().baseUrl(properties.url()).build();
    }

    public Optional<TodoUser> findUserByEmail(Email email) {
        try {
            List<TodoUser> users =
                    restClient
                            .get()
                            .uri(
                                    uriBuilder ->
                                            uriBuilder
                                                    .path("/users")
                                                    .queryParam("email", email.email())
                                                    .build())
                            .retrieve()
                            .body(USER_LIST_TYPE);
            return ofNullable(users).map(List::stream).flatMap(Stream::findAny);
        } catch (RestClientException e) {
            throw new TodoException("Failed to look up user by email at JSONPlaceholder", e);
        }
    }

    public TodoUser createUser(TodoUser user) {
        try {
            TodoUser created =
                    restClient.post().uri("/users").body(user).retrieve().body(TodoUser.class);
            if (created == null) {
                throw new TodoException("JSONPlaceholder returned empty body for user creation");
            }
            return created;
        } catch (RestClientException e) {
            throw new TodoException("Failed to create user at JSONPlaceholder", e);
        }
    }

    public Todo createTodo(Todo todo) {
        try {
            Todo created = restClient.post().uri("/todos").body(todo).retrieve().body(Todo.class);
            if (created == null) {
                throw new TodoException("JSONPlaceholder returned empty body for todo creation");
            }
            return created;
        } catch (RestClientException e) {
            throw new TodoException("Failed to create todo at JSONPlaceholder", e);
        }
    }
}
