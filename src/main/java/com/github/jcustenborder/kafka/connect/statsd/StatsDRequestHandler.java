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

import com.github.jcustenborder.kafka.connect.utils.data.SourceRecordConcurrentLinkedDeque;
import com.github.jcustenborder.netty.statsd.Metric;
import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

class StatsDRequestHandler extends SimpleChannelInboundHandler<Metric> {
  private static final Logger log = LoggerFactory.getLogger(StatsDRequestHandler.class);
  private final Time time;
  private final StatsDSourceConnectorConfig config;
  private final SourceRecordConcurrentLinkedDeque records;


  static final Schema KEY_SCHEMA;
  static final Schema VALUE_SCHEMA;

  static final String FIELD_SENDER = "sender";
  static final String FIELD_NAME = "name";
  static final String FIELD_RECIPIENT = "recipient";
  static final String FIELD_TYPE = "type";
  static final String FIELD_VALUE = "value";
  static final String FIELD_SAMPLERATE = "sampleRate";
  static final String FIELD_TIMESTAMP = "timestamp";
  static final Map<String, Object> SOURCE_PARTITION = ImmutableMap.of();
  static final Map<String, Object> SOURCE_OFFSET = ImmutableMap.of();

  static {
    KEY_SCHEMA = SchemaBuilder.struct()
        .name("com.github.jcustenborder.kafka.connect.statsd.StatsDKey")
        .field(FIELD_SENDER, SchemaBuilder.string().optional().doc("Remote address of the host sending data.").build())
        .field(FIELD_NAME, SchemaBuilder.string().doc("Name of the metric.").build())
        .build();

    VALUE_SCHEMA = SchemaBuilder.struct()
        .name("com.github.jcustenborder.kafka.connect.statsd.StatsDValue")
        .field(FIELD_SENDER, SchemaBuilder.string().optional().doc("Remote address of the host sending data.").build())
        .field(FIELD_RECIPIENT, SchemaBuilder.string().optional().doc("Address of the host that received the data.").build())
        .field(FIELD_NAME, SchemaBuilder.string().doc("Name of the metric.").build())
        .field(FIELD_TYPE, SchemaBuilder.string().doc("Type of metric.").build())
        .field(FIELD_VALUE, SchemaBuilder.float64().doc("Value for the metric.").build())
        .field(FIELD_SAMPLERATE, SchemaBuilder.float64().optional().doc("Sample rate for the metric. Only valid for counters.").build())
        .field(FIELD_TIMESTAMP, Timestamp.builder().doc("Sample rate for the metric. Only valid for counters.").build())
        .build();
  }

  StatsDRequestHandler(StatsDSourceConnectorConfig config, SourceRecordConcurrentLinkedDeque records, Time time) {
    this.config = config;
    this.records = records;
    this.time = time;
  }


  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, Metric metric) throws Exception {
    log.trace("received {}", metric);

    long timestamp = this.time.milliseconds();

    Struct keyStruct = new Struct(KEY_SCHEMA)
        .put(FIELD_SENDER, metric.sender().getHostName())
        .put(FIELD_NAME, metric.name());

    Struct valueStruct = new Struct(VALUE_SCHEMA)
        .put(FIELD_SENDER, metric.sender().getHostName())
        .put(FIELD_RECIPIENT, metric.recipient().getHostName())
        .put(FIELD_NAME, metric.name())
        .put(FIELD_TYPE, metric.type().name())
        .put(FIELD_VALUE, metric.value())
        .put(FIELD_SAMPLERATE, metric.sampleRate())
        .put(FIELD_TIMESTAMP, new Date(timestamp));

    SourceRecord sourceRecord = new SourceRecord(
        SOURCE_PARTITION,
        SOURCE_OFFSET,
        this.config.topic,
        null,
        KEY_SCHEMA,
        keyStruct,
        VALUE_SCHEMA,
        valueStruct
    );
    this.records.add(sourceRecord);
  }
}
