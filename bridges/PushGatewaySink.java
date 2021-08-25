// camel-k: language=java
// camel-k: resource=push-gateway-sink-transform.groovy
// camel-k: dependency=camel-jackson
// camel-k: dependency=camel-groovy
// camel-k: dependency=mvn:io.prometheus:simpleclient_pushgateway:0.9.0
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.AggregationStrategies;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.processor.aggregate.ClosedCorrelationKeyException;

public class PushGatewaySink extends RouteBuilder {

  private static final String LOGGER_NAME = "org.apache.camel.examples.iot.PushGatewaySink";

  @BeanInject
  private PushGateway pushGateway;

  @Override
  public void configure() throws Exception {

      from("kafka:{{telemetry.destination.name}}")
        .routeId("pushGatewaySink")
        .filter()
          .simple("${body} == ${null} || ${body} == ''")
          .stop()
        .end()
        .log(LoggingLevel.DEBUG, LOGGER_NAME, "${body}")
        .unmarshal().json(JsonLibrary.Jackson, Map.class)
        .setProperty("PrometheusJobId").simple("${body[locationId]}")
        .setProperty("PrometheusInstanceId").simple("${body[rigId]}")
        .to("direct:aggregateAndEmit")
      ;

      from("direct:aggregateAndEmit")
        .routeId("aggregateAndEmit")
        .onException(ClosedCorrelationKeyException.class)
          .handled(true)
          .log(LoggingLevel.WARN, LOGGER_NAME, "Got a metric after I already pushed: key=${exchangeProperty.PrometheusJobId}:${exchangeProperty.PrometheusInstanceId}:${body[time]}")
        .end()
        .aggregate()
          .simple("${exchangeProperty.PrometheusJobId}:${exchangeProperty.PrometheusInstanceId}:${body[time]}")
          .completionTimeout(constant("{{telemetry.window:2500}}"))
          .closeCorrelationKeyOnCompletion(100000)
          .aggregationStrategy(AggregationStrategies.flexible().pick(body()).accumulateInCollection(ArrayList.class).storeInBody())
            .transform().groovy("resource:classpath:push-gateway-sink-transform.groovy")
            .process((Exchange exchange) -> {
              List<?> parameters = exchange.getIn().getBody(List.class);
              pushGateway.push((CollectorRegistry) parameters.get(0), (String) parameters.get(1), (Map) parameters.get(2));
            })
            //.to("bean:pushGateway?method=push(CollectorRegistry,String,Map)?")
        .end()
      ;
  }
}
