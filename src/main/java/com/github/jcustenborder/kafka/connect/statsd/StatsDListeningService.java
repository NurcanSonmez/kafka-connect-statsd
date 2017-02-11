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
import com.github.jcustenborder.netty.statsd.StatsDRequestDecoder;
import com.google.common.util.concurrent.AbstractExecutionThreadService;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.kafka.common.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class StatsDListeningService extends AbstractExecutionThreadService {
  private static final Logger log = LoggerFactory.getLogger(StatsDListeningService.class);
  private final Time time;
  private final StatsDSourceConnectorConfig config;
  private final SourceRecordConcurrentLinkedDeque records;


  EventLoopGroup bossGroup = new NioEventLoopGroup(1); // (1)
  Bootstrap b;
  ChannelFuture channelFuture;

  public StatsDListeningService(StatsDSourceConnectorConfig config, SourceRecordConcurrentLinkedDeque records, Time time) {
    this.config = config;
    this.records = records;
    this.time = time;
  }


  @Override
  protected void startUp() throws Exception {
    log.info("startUp");
    b = new Bootstrap();
    b.group(bossGroup)
        .channel(NioDatagramChannel.class)
        .handler(new ChannelInitializer<DatagramChannel>() {
          @Override
          protected void initChannel(DatagramChannel datagramChannel) throws Exception {
            ChannelPipeline channelPipeline = datagramChannel.pipeline();
            channelPipeline.addLast(
                new LoggingHandler("StatsD", LogLevel.TRACE),
                new StatsDRequestDecoder(),
                new StatsDRequestHandler(config, records, time)
            );
          }
        });


    log.info("Binding to {}", this.config.port);
    this.channelFuture = b.bind(this.config.port).sync();

    log.info("finished startup");
  }

  @Override
  protected void run() throws Exception {
    try {
      while (isRunning()) {
        this.channelFuture.channel().closeFuture().await(1, TimeUnit.SECONDS);
      }
    } catch (Exception ex) {
      log.error("Exception thrown", ex);
      throw ex;
    }
  }

  @Override
  protected void shutDown() throws Exception {
    log.info("shutDown");
    try {
      this.channelFuture.channel().close();
      this.channelFuture.channel().closeFuture().sync();
      bossGroup.shutdownGracefully();
    } catch (InterruptedException e) {
      log.error("exception thrown", e);
    }
  }

}