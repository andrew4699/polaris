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

import jakarta.ws.rs.client.Client;
import org.apache.polaris.core.entity.PolarisPrincipalSecrets;
import org.apache.polaris.service.auth.TokenUtils;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TestParamExtension implements ParameterResolver {
    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(PolarisConnectionExtension.PolarisToken.class)
                || parameterContext.getParameter().getType().equals(PolarisPrincipalSecrets.class)
                || parameterContext.getParameter().getType().equals(TestInfo.class)
                || parameterContext.getParameter().getType().equals(PolarisConnectionExtension.PolarisToken.class)
                || parameterContext.getParameter().getType().equals(SnowmanCredentialsExtension.SnowmanCredentials.class)
                || parameterContext.getParameter().getType().equals(TestEnvironment.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return null;
    }
}
