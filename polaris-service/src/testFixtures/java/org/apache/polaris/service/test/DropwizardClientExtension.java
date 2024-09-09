/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.polaris.service.test;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.apache.polaris.core.entity.PolarisPrincipalSecrets;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static org.apache.polaris.service.test.PolarisConnectionExtension.findDropwizardExtension;

public class DropwizardClientExtension implements BeforeAllCallback, ParameterResolver {
    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
//        DropwizardAppExtension dropwizardAppExtension = findDropwizardExtension(extensionContext);
//        if (dropwizardAppExtension == null) {
//            return;
//        }
//
//        var store = extensionContext.getStore(ExtensionContext.Namespace.create(extensionContext.getRequiredTestClass()));
//        store.put(PolarisConnectionExtension.BASE_URL_PROPERTY, String.format("http://localhost:%d", dropwizardAppExtension.getLocalPort()));
//        store.put(PolarisConnectionExtension.API_CLIENT_PROPERTY, dropwizardAppExtension.client());
    }

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(TestEnvironment.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        try {
            DropwizardAppExtension dropwizardAppExtension = findDropwizardExtension(extensionContext);
            if (dropwizardAppExtension == null) {
                return null;
            }
            var env = new TestEnvironment();
            env.apiClient = dropwizardAppExtension.client();
            env.baseUrl = String.format("http://localhost:%d", dropwizardAppExtension.getLocalPort());
            return env;
        } catch(IllegalAccessException e) {
            throw new ParameterResolutionException(e.getMessage());
        }
    }
}
