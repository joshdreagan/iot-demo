// camel-k: language=java
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;

public class ArtemisSource extends RouteBuilder {

  private static final String LOGGER_NAME = "org.apache.camel.examples.iot.ArtemisSource";

  @Override
  public void configure() throws Exception {

      from("amqp:{{amqp.destination.type:topic}}:{{telemetry.destination.name}}?disableReplyTo=true")
        .routeId("artemisSource")
        .log(LoggingLevel.DEBUG, LOGGER_NAME, "${body}")
        .setHeader(KafkaConstants.KEY).jsonpath("concat($.locationId, \":\" ,$.rigId)", String.class)
        .log(LoggingLevel.DEBUG, LOGGER_NAME, String.format("Partition Key: ${headers[%s]}", KafkaConstants.KEY))
        .to("kafka:{{telemetry.destination.name}}")
      ;
  }
}
