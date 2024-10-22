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
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit test extension that constructs a TestEnvironment.
 * It tries to use environment variables and falls back to communicating with the local Dropwizard instance.
 */
public class TestEnvironmentExtension implements ParameterResolver {
  /**
   * Environment variable that specifies the Polaris base URL to use.
   * If this is not set, falls back to the root path of your Dropwizard instance.
   */
  public static final String ENV_BASE_URL = "INTEGRATION_TEST_BASE_URL";

  /**
   * Environment variable that specifies the HTTP client factory that should construct test HTTP clients.
   * If this is not set, falls back to the client created by Dropwizard.
   */
  public static final String ENV_HTTP_CLIENT_FACTORY_IMPL =
      "INTEGRATION_TEST_HTTP_CLIENT_FACTORY_IMPL";

  private static TestEnvironment env;

  public static synchronized TestEnvironment getEnv(ExtensionContext extensionContext)
      throws IllegalAccessException {
    if (env == null) {
      Optional<String> baseUrl = Optional.ofNullable(System.getenv(ENV_BASE_URL));
      DropwizardAppExtension dropwizardAppExtension = findDropwizardExtension(extensionContext);
      if (dropwizardAppExtension == null && baseUrl.isEmpty()) {
        throw new ParameterResolutionException(
            "No test URL specified. Tried to default to Dropwizard but could not find DropwizardAppExtension.");
      }

      env =
          new TestEnvironment(
              getHttpClient(dropwizardAppExtension),
              baseUrl.orElse(
                  String.format("http://localhost:%d", dropwizardAppExtension.getLocalPort())));
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
      return getEnv(extensionContext);
    } catch (IllegalAccessException e) {
      throw new ParameterResolutionException(e.getMessage());
    }
  }

  private static Client getHttpClient(DropwizardAppExtension dropwizardAppExtension) {
    Optional<String> httpClientImpl =
        Optional.ofNullable(System.getenv(ENV_HTTP_CLIENT_FACTORY_IMPL));

    if (httpClientImpl.isEmpty()) {
      if (dropwizardAppExtension == null) {
        throw new ParameterResolutionException(
            "Tried to default to Dropwizard client but could not find DropwizardAppExtension.");
      }
      return dropwizardAppExtension.client();
    }

    TestHttpClientFactory httpClientFactory;
    try {
      httpClientFactory =
          (TestHttpClientFactory)
              (Class.forName(httpClientImpl.get()).getDeclaredConstructor().newInstance());
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot initialize http Client, %s does not implement PolarisTestHttpClientFactory.",
              httpClientImpl),
          e);
    }
    return httpClientFactory.buildClient();
  }
}
