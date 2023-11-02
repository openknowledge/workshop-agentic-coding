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
package de.openknowledge.customermanagement.infrastructure.http;

import static java.net.URI.create;
import static java.util.Collections.singletonMap;
import static org.springframework.http.ProblemDetail.forStatusAndDetail;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(f -> Map.of("name", f.getField(), "reason", f.getDefaultMessage()))
                        .toList();
        return handleInvalidParams(ex.getBody(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex) {
        List<Map<String, String>> errors =
                ex.getConstraintViolations().stream()
                        .map(
                                v ->
                                        Map.<String, String>of(
                                                "name",
                                                v.getPropertyPath().toString(),
                                                "reason",
                                                v.getMessage()))
                        .toList();

        return handleInvalidParams(
                forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage()), errors);
    }

    private ResponseEntity<ProblemDetail> handleInvalidParams(
            ProblemDetail problem, List<Map<String, String>> errors) {
        problem.setType(create("validation-error"));
        problem.setProperties(singletonMap("invalid-params", errors));
        return ResponseEntity.badRequest().body(problem);
    }
}
