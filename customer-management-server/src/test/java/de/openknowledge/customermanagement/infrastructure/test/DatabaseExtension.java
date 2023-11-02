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
package de.openknowledge.customermanagement.infrastructure.test;

import static org.testcontainers.DockerClientFactory.lazyClient;

import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

public class DatabaseExtension implements BeforeAllCallback {

    private static final Logger LOG = LogManager.getLogger(DatabaseExtension.class);

    private static JdbcDatabaseContainer<?> database;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // uncomment this, when an older docker client should be used.
        // System.setProperty("api.version", "1.44");
        if (database == null) {
            var containerIds =
                    lazyClient().listContainersCmd().exec().stream()
                            .map(com.github.dockerjava.api.model.Container::getId);
            database = new PostgreSQLContainer<>("postgres:17");
            database.withReuse(true).start();
            String containerId = database.getContainerId();
            if (!isReused(containerId, containerIds)) {
                Flyway flyway =
                        Flyway.configure()
                                .dataSource(
                                        database.getJdbcUrl(),
                                        database.getUsername(),
                                        database.getPassword())
                                .cleanDisabled(false)
                                .load();
                flyway.clean();
                flyway.migrate();
                ScriptUtils.runInitScript(
                        new JdbcDatabaseDelegate(database, ""), "db/testdata.sql");
            } else {
                LOG.info("Reusing database container");
            }
        }
        System.setProperty("spring.datasource.url", database.getJdbcUrl());
        System.setProperty("spring.datasource.username", database.getUsername());
        System.setProperty("spring.datasource.password", database.getPassword());
    }

    private boolean isReused(String containerId, Stream<String> containerIds) {
        return containerIds.anyMatch(containerId::equals);
    }
}
