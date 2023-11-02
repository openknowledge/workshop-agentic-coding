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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class WireMockExtension implements BeforeAllCallback, BeforeEachCallback {

    private static final Logger LOG = LogManager.getLogger(WireMockExtension.class);

    private static WireMockServer wireMock;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (wireMock == null) {
            wireMock =
                    new WireMockServer(
                            options().dynamicPort().usingFilesUnderDirectory("../wiremock"));
            wireMock.start();
            System.setProperty("todo.url", wireMock.baseUrl());
            LOG.info("WireMock started.");
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        LOG.debug("resetRequests()");
        wireMock.resetRequests();
    }
}
