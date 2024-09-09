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

import static org.apache.polaris.service.context.DefaultContextResolver.REALM_PROPERTY_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.apache.polaris.core.context.CallContext;
import org.apache.polaris.core.context.RealmContext;
import org.apache.polaris.core.entity.PolarisEntityConstants;
import org.apache.polaris.core.entity.PolarisEntitySubType;
import org.apache.polaris.core.entity.PolarisEntityType;
import org.apache.polaris.core.entity.PolarisGrantRecord;
import org.apache.polaris.core.entity.PolarisPrincipalSecrets;
import org.apache.polaris.core.persistence.MetaStoreManagerFactory;
import org.apache.polaris.core.persistence.PolarisMetaStoreManager;
import org.apache.polaris.service.auth.TokenUtils;
import org.apache.polaris.service.config.PolarisApplicationConfig;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.LoggerFactory;

public class PolarisConnectionExtension implements BeforeAllCallback, ParameterResolver {

  // TODO: MOVE THESE
  public static final String BASE_URL_PROPERTY = "base_url";
  public static final String API_CLIENT_PROPERTY = "api_client";

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private MetaStoreManagerFactory metaStoreManagerFactory;
  private DropwizardAppExtension dropwizardAppExtension;

  public record PolarisToken(String token) {}

  private static PolarisPrincipalSecrets adminSecrets;
  private static String realm;

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    dropwizardAppExtension = findDropwizardExtension(extensionContext);
    if (dropwizardAppExtension == null) {
      return;
    }

    // Generate unique realm using test name for each test since the tests can run in parallel
    realm = getTestRealm(extensionContext.getRequiredTestClass());
    extensionContext
        .getStore(Namespace.create(extensionContext.getRequiredTestClass()))
        .put(REALM_PROPERTY_KEY, realm);

    try {
      PolarisApplicationConfig config =
          (PolarisApplicationConfig) dropwizardAppExtension.getConfiguration();
      metaStoreManagerFactory = config.getMetaStoreManagerFactory();

      RealmContext realmContext =
          config
              .getRealmContextResolver()
              .resolveRealmContext(
                  "http://localhost", "GET", "/", Map.of(), Map.of(REALM_PROPERTY_KEY, realm));
      CallContext ctx =
          config
              .getCallContextResolver()
              .resolveCallContext(realmContext, "GET", "/", Map.of(), Map.of());
      CallContext.setCurrentContext(ctx);
      PolarisMetaStoreManager metaStoreManager =
          metaStoreManagerFactory.getOrCreateMetaStoreManager(ctx.getRealmContext());
      PolarisMetaStoreManager.EntityResult principal =
          metaStoreManager.readEntityByName(
              ctx.getPolarisCallContext(),
              null,
              PolarisEntityType.PRINCIPAL,
              PolarisEntitySubType.NULL_SUBTYPE,
              PolarisEntityConstants.getRootPrincipalName());

      Map<String, String> propertiesMap = readInternalProperties(principal);
      adminSecrets =
          metaStoreManager
              .loadPrincipalSecrets(ctx.getPolarisCallContext(), propertiesMap.get("client_id"))
              .getPrincipalSecrets();
    } finally {
      CallContext.unsetCurrentContext();
    }
  }

  public static String getTestRealm(Class testClassName) {
    return testClassName.getName().replace('.', '_');
  }

  static PolarisPrincipalSecrets getAdminSecrets() {
    return adminSecrets;
  }

  public static @Nullable DropwizardAppExtension findDropwizardExtension(
      ExtensionContext extensionContext) throws IllegalAccessException {
    Field dropwizardExtensionField =
        findAnnotatedFields(extensionContext.getRequiredTestClass(), true);
    if (dropwizardExtensionField == null) {
      LoggerFactory.getLogger(PolarisGrantRecord.class)
          .warn(
              "Unable to find dropwizard extension field in test class {}",
              extensionContext.getRequiredTestClass());
      return null;
    }
    DropwizardAppExtension appExtension =
        (DropwizardAppExtension) ReflectionUtils.makeAccessible(dropwizardExtensionField).get(null);
    return appExtension;
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(PolarisToken.class)
        || parameterContext.getParameter().getType().equals(PolarisPrincipalSecrets.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (parameterContext.getParameter().getType().equals(PolarisToken.class)) {
      String token =
          TokenUtils.getTokenFromSecrets(
              dropwizardAppExtension.client(),
              String.format("http://localhost:%d", dropwizardAppExtension.getLocalPort()),
              adminSecrets.getPrincipalClientId(),
              adminSecrets.getMainSecret(),
              realm);
      return new PolarisToken(token);
    } else {
      return metaStoreManagerFactory;
    }
  }

  private static Map<String, String> readInternalProperties(
      PolarisMetaStoreManager.EntityResult principal) {
    try {
      return OBJECT_MAPPER.readValue(
          principal.getEntity().getInternalProperties(),
          new TypeReference<Map<String, String>>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static Field findAnnotatedFields(Class<?> testClass, boolean isStaticMember) {
    final Optional<Field> set =
        Arrays.stream(testClass.getDeclaredFields())
            .filter(m -> isStaticMember == Modifier.isStatic(m.getModifiers()))
            .filter(m -> DropwizardAppExtension.class.isAssignableFrom(m.getType()))
            .findFirst();
    if (set.isPresent()) {
      return set.get();
    }
    if (!testClass.getSuperclass().equals(Object.class)) {
      return findAnnotatedFields(testClass.getSuperclass(), isStaticMember);
    }
    return null;
  }
}
