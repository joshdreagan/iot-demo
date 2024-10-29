/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.examples;

import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CamelRouteConfiguration extends RouteBuilder {

  private static final Logger log = LoggerFactory.getLogger(CamelRouteConfiguration.class);

  @Override
  public void configure() {

    //@formatter:off
    from("amqp:topic:{{bridge.source.name}}?disableReplyTo=true&connectionFactory=#jmsConnectionFactory")
      .routeId("artemisSource")
      .log(LoggingLevel.DEBUG, log, "${body}")
      .setHeader(KafkaConstants.KEY).jsonpath("concat($.locationId, \":\" ,$.rigId)", String.class)
      .log(LoggingLevel.DEBUG, log, String.format("Partition Key: ${headers[%s]}", KafkaConstants.KEY))
      .to(ExchangePattern.InOnly, "kafka:{{bridge.sink.name}}")
    ;
    //@formatter:on
  }
}
