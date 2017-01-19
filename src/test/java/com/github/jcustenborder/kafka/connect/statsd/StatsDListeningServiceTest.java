/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
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
package com.github.jcustenborder.kafka.connect.statsd;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ServiceManager;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import io.confluent.kafka.connect.utils.data.SourceRecordConcurrentLinkedDeque;
import org.apache.kafka.common.utils.Time;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatsDListeningServiceTest {
  private static final Logger log = LoggerFactory.getLogger(StatsDListeningServiceTest.class);

  ServiceManager serviceManager;
  StatsDSourceConnectorConfig config;
  SourceRecordConcurrentLinkedDeque records;
  Time time;

  @BeforeEach
  public void before() throws TimeoutException {
    log.info("before");
    this.config = new StatsDSourceConnectorConfig(StatsDSourceConnectorConfigTest.settings());
    this.time = mock(Time.class);
    when(this.time.milliseconds()).thenReturn(1484693116123L);
    this.records = new SourceRecordConcurrentLinkedDeque();
    this.serviceManager = new ServiceManager(Arrays.asList(new StatsDListeningService(config, records, time)));
    this.serviceManager.startAsync();
    this.serviceManager.awaitHealthy(15, TimeUnit.SECONDS);
  }

  @Test
  public void test() throws InterruptedException {

    StatsDClient statsd = new NonBlockingStatsDClient(this.getClass().getName(), "127.0.0.1", 8125);
    log.info("test");
    Stopwatch stopwatch = Stopwatch.createStarted();
    for (int i = 0; i < 1000; i++) {
      statsd.gauge("guage", 1000L);
      Thread.sleep(1);
    }
    statsd.time("test", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    Thread.sleep(2000);

    log.info("test received {} record(s)", this.records.size());
  }

  @AfterEach
  public void after() throws TimeoutException {
    log.info("after");
    this.serviceManager.stopAsync();
    this.serviceManager.awaitStopped(60, TimeUnit.SECONDS);
  }
}
