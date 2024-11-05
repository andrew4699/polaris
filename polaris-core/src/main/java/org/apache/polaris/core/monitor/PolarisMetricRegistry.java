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
package org.apache.polaris.core.monitor;

import com.google.common.collect.Iterables;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.apache.polaris.core.resource.TimedApi;

/**
 * Wrapper around the Micrometer {@link MeterRegistry} providing additional metric management
 * functions for the Polaris application. Implements in-memory caching of timers and counters.
 * Records two metrics for each instrument with one tagged by the realm ID (realm-specific metric)
 * and one without. The realm-specific metric is suffixed with ".realm".
 */
public class PolarisMetricRegistry {
  private final MeterRegistry meterRegistry;
  private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
  private static final String TAG_REALM = "REALM_ID";
  private static final String TAG_RESP_CODE = "HTTP_RESPONSE_CODE";
  private static final String TAG_API_NAME = "API_NAME";
  private static final String SUFFIX_COUNTER = ".count";
  private static final String SUFFIX_ERROR = ".error";
  private static final String SUFFIX_REALM = ".realm";
  private static final String METRIC_TIMEDAPI_COUNT = "polaris.TimedApi" + SUFFIX_COUNTER;
  private static final String METRIC_TIMEDAPI_ERROR = "polaris.TimedApi" + SUFFIX_ERROR;

  public PolarisMetricRegistry(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    new ClassLoaderMetrics().bindTo(meterRegistry);
    new JvmMemoryMetrics().bindTo(meterRegistry);
    new JvmGcMetrics().bindTo(meterRegistry);
    new ProcessorMetrics().bindTo(meterRegistry);
    new JvmThreadMetrics().bindTo(meterRegistry);
  }

  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  public void init(Class<?>... classes) {
    for (Class<?> clazz : classes) {
      Method[] methods = clazz.getDeclaredMethods();
      for (Method method : methods) {
        if (method.isAnnotationPresent(TimedApi.class)) {
          TimedApi timedApi = method.getAnnotation(TimedApi.class);
          String metric = timedApi.value();
          computeTimerIfAbsent(metric, Collections.emptyList());
          computeCounterIfAbsent(metric + SUFFIX_COUNTER, Collections.emptyList());

          // Error counters contain the HTTP response code in a tag, thus caching them would not be
          // meaningful.
          computeCounterIfAbsent(
              metric + SUFFIX_ERROR, Collections.singleton(Tag.of(TAG_RESP_CODE, "400")));
          computeCounterIfAbsent(
              metric + SUFFIX_ERROR, Collections.singleton(Tag.of(TAG_RESP_CODE, "500")));
        }
      }
    }
  }

  public void recordTimer(String metric, long elapsedTimeMs, String realmId) {
    Timer timer = computeTimerIfAbsent(metric, Collections.emptyList());
    timer.record(elapsedTimeMs, TimeUnit.MILLISECONDS);

    Timer timerRealm =
        computeTimerIfAbsent(
            metric + SUFFIX_REALM, Collections.singleton(Tag.of(TAG_REALM, realmId)));
    timerRealm.record(elapsedTimeMs, TimeUnit.MILLISECONDS);
  }

  public void incrementCounter(String metric, String realmId) {
    incrementCounter(metric + SUFFIX_COUNTER, realmId, Collections.emptyList());
  }

  public void incrementErrorCounter(String metric, int statusCode, String realmId) {
    incrementCounter(
        metric + SUFFIX_ERROR, realmId, Tag.of(TAG_RESP_CODE, String.valueOf(statusCode)));
  }

  public void incrementTimedApiCounter(String apiName, String realmId) {
    incrementCounter(METRIC_TIMEDAPI_COUNT, realmId, Tag.of(TAG_API_NAME, apiName));
  }

  public void incrementTimedApiErrorCounter(String apiName, int statusCode, String realmId) {
    incrementCounter(
        METRIC_TIMEDAPI_ERROR,
        realmId,
        List.of(Tag.of(TAG_API_NAME, apiName), Tag.of(TAG_RESP_CODE, String.valueOf(statusCode))));
  }

  private void incrementCounter(String name, String realmId, Tag tag) {
    incrementCounter(name, realmId, Collections.singleton(tag));
  }

  private void incrementCounter(String name, String realmId, Iterable<Tag> tags) {
    computeCounterIfAbsent(name, tags).increment();
    computeCounterIfAbsent(
        name + SUFFIX_REALM,
        Iterables.concat(Collections.singleton(Tag.of(TAG_REALM, realmId)), tags));
  }

  private Counter computeCounterIfAbsent(String name, Iterable<Tag> tags) {
    return counters.computeIfAbsent(
        name, m -> Counter.builder(name).tags(tags).register(meterRegistry));
  }

  private Timer computeTimerIfAbsent(String name, Iterable<Tag> tags) {
    return timers.computeIfAbsent(
        name, m -> Timer.builder(name).tags(tags).register(meterRegistry));
  }
}
