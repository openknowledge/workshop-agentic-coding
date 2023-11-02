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
package de.openknowledge.customermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import de.openknowledge.customermanagement.infrastructure.test.IntegrationTest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@IntegrationTest
class CustomerControllerTest {

    @LocalServerPort private int port;

    @Autowired private RestClient.Builder clientBuilder;

    @BeforeEach
    void setup() {
        clientBuilder.baseUrl("http://localhost:" + port + "/api");
    }

    @Test
    void multipleAdditionalEmails() throws Exception {
        // Given
        RestClient client = clientBuilder.build();

        // When
        var createResponse =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {
                                    "name": {
                                        "firstName": "Max",
                                        "lastName": "Mustermann"
                                    },
                                    "email": "max.mustermann@example.com",
                                    "phoneNumber": "+49 123 4567890",
                                    "birthDate": "1990-05-15",
                                    "additionalEmails": [
                                        "max.mustermann@example2.com"
                                    ],
                                    "additionalPhoneNumbers": [
                                        {
                                            "type": "PRIVATE",
                                            "phoneNumber": "+49157123456"
                                        }
                                    ],
                                    "externalUserId": 0
                                }
                            """)
                        .retrieve()
                        .toBodilessEntity();

        // Then
        assertThat(createResponse.getStatusCode().value()).isEqualTo(CREATED.value());
    }

    @Test
    void createAndGetCustomer() throws Exception {
        // Given
        RestClient client = clientBuilder.build();

        // When
        var createResponse =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {
                                    "name": {
                                        "firstName": "Max",
                                        "lastName": "Mustermann"
                                    },
                                    "email": "max.mustermann@example.com",
                                    "phoneNumber": "+49 123 4567890",
                                    "birthDate": "1990-05-15"
                                }
                                """)
                        .retrieve()
                        .toBodilessEntity();

        // Then
        assertThat(createResponse.getStatusCode().value()).isEqualTo(CREATED.value());
        URI location = createResponse.getHeaders().getLocation();
        assertThat(location).isNotNull();

        // When
        Map<String, Object> body =
                client.get()
                        .uri(location)
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        // Then
        Map<String, Object> name = (Map<String, Object>) body.get("name");
        assertThat(name.get("firstName")).isEqualTo("Max");
        assertThat(name.get("lastName")).isEqualTo("Mustermann");
        assertThat(body.get("email")).isEqualTo("max.mustermann@example.com");
        assertThat(body.get("externalUserId")).isEqualTo(11);
    }

    @Test
    void createInvalidCustomerFails() throws Exception {
        // Given
        RestClient client = clientBuilder.build();

        // When
        var exception =
                assertThrows(
                        HttpClientErrorException.BadRequest.class,
                        () ->
                                client.post()
                                        .uri("/customers")
                                        .contentType(APPLICATION_JSON)
                                        .body(
                                                """
                                {
                                    "name": {
                                        "firstName": "Max1"
                                    },
                                    "phoneNumber": "+49+123 4567890",
                                    "birthDate": "1990-05-15"
                                }
                                """)
                                        .retrieve()
                                        .toBodilessEntity());

        // Then
        assertThat(exception.getStatusCode().value()).isEqualTo(BAD_REQUEST.value());

        // When
        Map<String, Object> body =
                exception.getResponseBodyAs(
                        new ParameterizedTypeReference<Map<String, Object>>() {});

        // Then
        assertThat(body).contains(entry("instance", "/api/customers"));
        assertThat(body).containsKey("invalid-params");
        List<Map<String, String>> invalidParams =
                (List<Map<String, String>>) body.get("invalid-params");
        assertThat(invalidParams)
                .containsExactlyInAnyOrder(
                        Map.of("name", "name.firstName", "reason", "darf keine Ziffern enthalten"),
                        Map.of("name", "name.lastName", "reason", "darf nicht null sein"),
                        Map.of(
                                "name",
                                "phoneNumber",
                                "reason",
                                "darf nur Ziffern, Klammern, Slashes oder Bindestriche enthalten und optional mit + beginnen"),
                        Map.of("name", "email", "reason", "darf nicht null sein"));
    }

    @Test
    void createCustomerWithInvalidAdditionalParametersFails() throws Exception {
        // Given
        RestClient client = clientBuilder.build();

        // When
        var exception =
                assertThrows(
                        HttpClientErrorException.BadRequest.class,
                        () ->
                                client.post()
                                        .uri("/customers")
                                        .contentType(APPLICATION_JSON)
                                        .body(
                                                """
                                {
                                    "name": {
                                        "firstName": "Max",
                                        "lastName": "Mustermann"
                                    },
                                    "email": "max.mustermann@openknowledge.de",
                                    "phoneNumber": "+49 123 4567890",
                                    "birthDate": "1990-05-15",
                                    "additionalEmails": ["max.mustermann(at)gmail.com"],
                                    "additionalPhoneNumbers": [
                                        {
                                            "type": "BUSINESS",
                                            "phoneNumber": "+49+123 4567890"
                                        },
                                        {
                                            "type": "HOLIDAY",
                                            "phoneNumber": "+49 123 4567890"
                                        }
                                    ]
                                }
                                """)
                                        .retrieve()
                                        .toBodilessEntity());

        // Then
        assertThat(exception.getStatusCode().value()).isEqualTo(BAD_REQUEST.value());

        // When
        Map<String, Object> body =
                exception.getResponseBodyAs(
                        new ParameterizedTypeReference<Map<String, Object>>() {});

        // Then
        assertThat(body).contains(entry("instance", "/api/customers"));
        assertThat(body).containsKey("invalid-params");
        List<Map<String, String>> invalidParams =
                (List<Map<String, String>>) body.get("invalid-params");
        assertThat(invalidParams)
                .containsExactlyInAnyOrder(
                        Map.of(
                                "name",
                                "additionalPhoneNumbers[0].phoneNumber",
                                "reason",
                                "darf nur Ziffern, Klammern, Slashes oder Bindestriche enthalten und optional mit + beginnen"),
                        Map.of(
                                "name",
                                "additionalPhoneNumbers[1].type",
                                "reason",
                                "muss einer der folgenden Werte sein: [PRIVATE, BUSINESS, MOBILE]"),
                        Map.of(
                                "name",
                                "additionalEmails[0]",
                                "reason",
                                "muss eine korrekt formatierte E-Mail-Adresse sein"));
    }

    @Test
    void createCustomerWithExistingExternalUserSkipsUserCreation() throws Exception {
        // Given
        RestClient client = clientBuilder.build();

        // When
        var createResponse =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {"name":{"firstName":"Known","lastName":"User"},"email":"known@example.com"}
                                """)
                        .retrieve()
                        .toBodilessEntity();

        // Then
        assertThat(createResponse.getStatusCode().value()).isEqualTo(CREATED.value());
        Map<String, Object> body =
                client.get()
                        .uri(createResponse.getHeaders().getLocation())
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        assertThat(body.get("externalUserId")).isEqualTo(7);
    }

    @Test
    void createCustomerRollsBackWhenLookupFails() throws Exception {
        // Given
        RestClient client = clientBuilder.build();

        // When
        var response =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {"name":{"firstName":"Fail","lastName":"Lookup"},"email":"fail-lookup@example.com"}
                                """)
                        .retrieve()
                        .onStatus(status -> status.value() == 502, (req, res) -> {})
                        .toBodilessEntity();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(502);
        String listBody =
                client.get()
                        .uri("/customers")
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);
        assertThat(listBody).doesNotContain("fail-lookup@example.com");
    }

    @Test
    void createCustomerRollsBackWhenUserCreationFails() throws Exception {
        // Given
        RestClient client = clientBuilder.build();

        // When
        var response =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {"name":{"firstName":"Fail","lastName":"CreateUser"},"email":"fail-create-user@example.com"}
                                """)
                        .retrieve()
                        .onStatus(status -> status.value() == 502, (req, res) -> {})
                        .toBodilessEntity();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(502);
        String listBody =
                client.get()
                        .uri("/customers")
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);
        assertThat(listBody).doesNotContain("fail-create-user@example.com");
    }

    @Test
    void createCustomerRollsBackWhenTodoCreationFails() throws Exception {
        // Given
        RestClient client = clientBuilder.build();

        // When
        var response =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {"name":{"firstName":"Fail","lastName":"Todo"},"email":"fail-todo@example.com"}
                                """)
                        .retrieve()
                        .onStatus(status -> status.value() == 502, (req, res) -> {})
                        .toBodilessEntity();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(502);
        String listBody =
                client.get()
                        .uri("/customers")
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);
        assertThat(listBody).doesNotContain("fail-todo@example.com");
    }

    @Test
    void listCustomers() throws Exception {
        // Given
        RestClient client = clientBuilder.build();
        var createResponse =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {
                                    "name": {
                                        "firstName": "Erika",
                                        "lastName": "Musterfrau"
                                    },
                                    "email": "erika.musterfrau@example.com"
                                }
                                """)
                        .retrieve()
                        .toBodilessEntity();
        assertTrue(createResponse.getStatusCode().is2xxSuccessful(), "Creation successful");

        // When
        String listBody =
                client.get()
                        .uri("/customers")
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

        // Then
        assertThat(listBody).contains("erika.musterfrau@example.com");
    }

    @Test
    void updateCustomer() throws Exception {
        // Given
        RestClient client = clientBuilder.build();
        var createResponse =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {"name":{"firstName":"Before","lastName":"Update"},"email":"update-test@example.com"}
                                """)
                        .retrieve()
                        .toBodilessEntity();

        // When
        var updateResponse =
                client.put()
                        .uri(createResponse.getHeaders().getLocation())
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {"name":{"firstName":"After","lastName":"Update"},"email":"update-test@example.com"}
                                """)
                        .retrieve()
                        .toBodilessEntity();
        assertTrue(updateResponse.getStatusCode().is2xxSuccessful(), "Update successful");

        // Then
        Map<String, Object> updated =
                client.get()
                        .uri(createResponse.getHeaders().getLocation())
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<String, Object> updatedName = (Map<String, Object>) updated.get("name");
        assertThat(updatedName.get("firstName")).isEqualTo("After");
        assertThat(updated.get("externalUserId")).isEqualTo(11);
    }

    @Test
    void deleteCustomer() throws Exception {
        // Given
        RestClient client = clientBuilder.build();
        var createResponse =
                client.post()
                        .uri("/customers")
                        .contentType(APPLICATION_JSON)
                        .body(
                                """
                                {
                                    "name": {
                                        "firstName": "ToDelete",
                                        "lastName": "User"
                                    },
                                    "email": "delete-test@example.com"
                                }
                                """)
                        .retrieve()
                        .toBodilessEntity();
        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
        URI location = createResponse.getHeaders().getLocation();
        assertThat(location).isNotNull();

        // When
        var deleteResponse = client.delete().uri(location).retrieve().toBodilessEntity();
        assertTrue(deleteResponse.getStatusCode().is2xxSuccessful(), "Delete successful");

        // Then -- GET should return 404
        var notFoundResponse =
                client.get()
                        .uri(location)
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .onStatus(status -> status.value() == 404, (req, res) -> {})
                        .toBodilessEntity();
        assertThat(notFoundResponse.getStatusCode().value()).isEqualTo(404);
    }
}
