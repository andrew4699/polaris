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

import static org.apache.polaris.service.test.PolarisConnectionExtension.findDropwizardExtension;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.Client;
import java.util.Optional;
import org.apache.iceberg.common.DynConstructors;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TestEnvironmentExtension implements ParameterResolver {
  private static final String ENV_BASE_URL = "INTEGRATION_TEST_BASE_URL";
  private static final String ENV_HTTP_CLIENT_IMPL = "INTEGRATION_TEST_HTTP_CLIENT_IMPL";

  private static TestEnvironment env;

  public static TestEnvironment getEnv(ExtensionContext extensionContext)
      throws IllegalAccessException {
    if (env == null) {
      DropwizardAppExtension dropwizardAppExtension = findDropwizardExtension(extensionContext);
      if (dropwizardAppExtension == null) {
        throw new RuntimeException(
            "Must specify a custom TestEnvironment or have a DropwizardAppExtension");
      }

      TestEnvironment dropwizardEnv = new TestEnvironment();
      dropwizardEnv.apiClient = dropwizardAppExtension.client();
      dropwizardEnv.baseUrl =
          String.format("http://localhost:%d", dropwizardAppExtension.getLocalPort());
      return dropwizardEnv;
    }
    return env;
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
      var baseUrl = Optional.ofNullable(System.getenv("INTEGRATION_TEST_BASE_URL"));

      DropwizardAppExtension dropwizardAppExtension = findDropwizardExtension(extensionContext);
      if (dropwizardAppExtension == null && baseUrl.isEmpty()) {
        throw new ParameterResolutionException(
            "No test URL specified. Tried to default to Dropwizard but could not find DropwizardAppExtension.");
      }

      env = new TestEnvironment();
      env.apiClient = getHttpClient(dropwizardAppExtension);
      env.baseUrl =
          baseUrl.orElse(
              String.format("http://localhost:%d", dropwizardAppExtension.getLocalPort()));
      return env;
    } catch (IllegalAccessException e) {
      throw new ParameterResolutionException(e.getMessage());
    }
  }

  private Client getHttpClient(DropwizardAppExtension dropwizardAppExtension) {
    var httpClientImpl = Optional.ofNullable(System.getenv("INTEGRATION_TEST_HTTP_CLIENT_IMPL"));

    if (httpClientImpl.isEmpty()) {
      if (dropwizardAppExtension == null) {
        throw new ParameterResolutionException(
            "Tried to default to Dropwizard client but could not find DropwizardAppExtension.");
      }
      return dropwizardAppExtension.client();
    }

    DynConstructors.Ctor<Client> ctor;
    try {
      ctor =
          DynConstructors.builder(Client.class)
              .loader(TestEnvironmentExtension.class.getClassLoader())
              .impl(httpClientImpl.get())
              .buildChecked();
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot initialize http Client implementation %s: %s",
              httpClientImpl, e.getMessage()),
          e);
    }

    try {
      return ctor.newInstance();
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot initialize http Client, %s does not implement Client.", httpClientImpl),
          e);
    }
  }
}
