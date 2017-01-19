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

import io.confluent.kafka.connect.utils.config.ValidPort;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

public class StatsDSourceConnectorConfig extends AbstractConfig {
  public final String topic;
  public final int port;

  public static final String TOPIC_CONF = "topic";
  static final String TOPIC_DOC = "The topic to write the data to.";

  public static final String PORT_CONF = "port";
  static final String PORT_DOC = "The port to listen on.";

  public StatsDSourceConnectorConfig(Map<String, String> m) {
    super(config(), m);
    this.topic = this.getString(TOPIC_CONF);
    this.port = this.getInt(PORT_CONF);
  }

  public static ConfigDef config() {
    return new ConfigDef()
        .define(TOPIC_CONF, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, TOPIC_DOC)
        .define(PORT_CONF, ConfigDef.Type.INT, 8125, ValidPort.of(1000, 65535), ConfigDef.Importance.HIGH, PORT_DOC);
  }
}
