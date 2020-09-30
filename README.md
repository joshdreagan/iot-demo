# IoT Demo

![Demo Architecture](./images/demo_architecture.png)

## Requirements

- [Apache Maven 3.x](http://maven.apache.org)
- [Red Hat AMQ Broker 7.x](https://developers.redhat.com/products/amq/overview)
- [Red Hat OpenShift 4.x](https://developers.redhat.com/products/openshift/getting-started)
- [Python 3.x](https://www.python.org/downloads/)

## Preparing

### AMQ Broker

- Install and run Red Hat AMQ Broker [https://developers.redhat.com/products/amq/hello-world]

  - _This should be done outside of OpenShift simulating the "Field Environment"._

### AMQ Streams

- Install the AMQ Streams Operator from OperatorHub

- Create and configure an AMQ Streams cluster

  ```
  oc apply -f ./kube/kafka-cluster.yaml -n iot-demo
  oc apply -f ./kube/kafka-topics.yaml -n iot-demo
  ```

### Prometheus

- Install the Prometheus Operator from OperatorHub

- Install the Prometheus Pushgateway

  ```
  oc new-app prom/pushgateway -n iot-demo
  ```

- Create and configure the Prometheus resources

  ```
  oc apply -f ./kube/prometheus.yaml -n iot-demo
  oc apply -f ./kube/prometheus-gateway-service-monitor.yaml -n iot-demo
  ```

### Grafana

- TODO

### Camel K

- Install the Camel K Operator from OperatorHub

- Configure the Camel K Integration Platform

  ```
  oc apply -f ./kube/integration-platform.yaml -n iot-demo
  ```

- Install the Camel K bridge applications

  - _Don't forget to modify the `ConfigMap` and `Secret` files with your environment settings._

  ```
  oc create configmap artemis-source-configmap --from-file=application.properties=./artemis-source-configmap.properties
  oc create secret generic artemis-source-secret --from-file=application.properties=./artemis-source-secret.properties
  kamel run \
    --namespace iot-demo \
    --configmap artemis-source-configmap \
    --secret artemis-source-secret \
    ArtemisSource.java

  oc create configmap push-gateway-sink-configmap --from-file=application.properties=./push-gateway-sink-configmap.properties
  kamel run \
    --namespace iot-demo \
    --configmap push-gateway-sink-configmap \
    PushGatewaySink.java
  ```

### Simulator

- You can run the Python script to simulate device telemetry being sent to the AMQ Broker

  ```
  python simulators/iot/pumpjack/sim.py --location-id field-01 --rig-id pumpjack-01 --broker-username admin --broker-password admin --telemetry-topic 'iot.telemetry' --telemetry-frequency 1 --buffer-timeout 10000 --verbose 'tcp://localhost:1883'
  ```