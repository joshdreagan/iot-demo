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

import io.prometheus.client.exporter.PushGateway;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.ClosedCorrelationKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;

@Component
public class CamelRouteConfiguration extends RouteBuilder {

  private static final Logger log = LoggerFactory.getLogger(CamelRouteConfiguration.class);

  @Autowired
  BridgeConfiguration config;

  @Bean
  @Autowired
  public PushGateway pushGateway(BridgeConfiguration bridgeConfiguration) {
    return new io.prometheus.client.exporter.PushGateway(String.format("%s:%d", config.sink().host(), config.sink().port()));
  }

  @Override
  public void configure() {

    //@formatter:off
    from("kafka:{{bridge.source.name}}")
      .routeId("pushGatewaySink")
      .filter()
        .simple("${body} == ${null} || ${body} == ''")
        .stop()
      .end()
      .log(LoggingLevel.DEBUG, log, "${body}")
      .unmarshal().json(JsonLibrary.Jackson, Map.class)
      .setProperty("PrometheusJobId").simple("${body[locationId]}")
      .setProperty("PrometheusInstanceId").simple("${body[rigId]}")
      .to("direct:aggregateAndEmit")
    ;

    from("direct:aggregateAndEmit")
      .routeId("aggregateAndEmit")
      .onException(ClosedCorrelationKeyException.class)
        .handled(true)
        .log(LoggingLevel.WARN, log, "Got a metric after I already pushed: key=${exchangeProperty.PrometheusJobId}:${exchangeProperty.PrometheusInstanceId}:${body[time]}")
      .end()
      .aggregate()
        .simple("${exchangeProperty.PrometheusJobId}:${exchangeProperty.PrometheusInstanceId}:${body[time]}")
        .completionTimeout(constant("{{bridge.source.window:2500}}"))
        .closeCorrelationKeyOnCompletion(100000)
        .aggregationStrategy(
          AggregationStrategies.flexible()
            .pick(body())
            .accumulateInCollection(ArrayList.class)
            .storeInBody()
        )
      .transform().groovy("resource:classpath:push-gateway-sink-transform.groovy")
      /*
      .process((Exchange exchange) -> {
        List<?> parameters = exchange.getIn().getBody(List.class);
        pushGateway.push(
          (CollectorRegistry) parameters.get(0),
          (String) parameters.get(1),
          (Map) parameters.get(2)
        );
      })
      */
      .to("bean:pushGateway?method=push(${body[0]},${body[1]},${body[2]})")
      .end()
    ;
    //@formatter:on
  }
}
