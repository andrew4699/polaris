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

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit test extension that determines the TestEnvironment. Falls back to targetting the local
 * Dropwizard instance.
 */
public class TestEnvironmentExtension implements ParameterResolver {
  private static TestEnvironment env;

  public static synchronized TestEnvironment getEnv(ExtensionContext extensionContext)
      throws IllegalAccessException {
    if (env == null) {
      env = new OtherTestEnvironmentResolver().resolveTestEnvironment(extensionContext);
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
}
