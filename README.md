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
  oc new-app prom/pushgateway -l 'app=iot-demo' -l 'prometheus/type=pushgateway' -n iot-demo
  ```

- Create and configure the Prometheus resources

  ```
  oc apply -f ./kube/prometheus.yaml -n iot-demo
  oc apply -f ./kube/prometheus-pushgateway-service-monitor.yaml -n iot-demo
  ```

### Grafana

- Install and configure a Grafana instance

  ```
  oc apply -f ./kube/grafana.yaml -n iot-demo
  ```

- Import the Grafana datasource and dashoard found in the `./grafana/` directory. You can either use the Web UI, or the REST API.

  - The Web UI can be found at:

    ```
    echo "http://$(oc get route grafana --template='{{.spec.host}}')/"
    ```

### Camel K

- Install the Camel K Operator from OperatorHub

- Configure the Camel K Integration Platform

  ```
  oc apply -f ./kube/integration-platform.yaml -n iot-demo
  ```

- Install the Camel K bridge applications

  - _Don't forget to modify the `ConfigMap` and `Secret` files with your environment settings._

  ```
  oc create configmap artemis-source-configmap --from-file=application.properties=./bridges/artemis-source-configmap.properties
  oc create secret generic artemis-source-secret --from-file=application.properties=./bridges/artemis-source-secret.properties
  kamel run \
    --namespace iot-demo \
    --configmap artemis-source-configmap \
    --secret artemis-source-secret \
    ./bridges/ArtemisSource.java

  oc create configmap push-gateway-sink-configmap --from-file=application.properties=./bridges/push-gateway-sink-configmap.properties
  kamel run \
    --namespace iot-demo \
    --configmap push-gateway-sink-configmap \
    ./bridges/PushGatewaySink.java
  ```

### Simulator

- You can run the Python script to simulate device telemetry being sent to the AMQ Broker

  ```
  python simulators/iot/pumpjack/sim.py --location-id field-01 --rig-id pumpjack-01 --broker-username admin --broker-password admin --telemetry-topic 'iot.telemetry' --telemetry-frequency 1 --buffer-timeout 10000 --verbose 'tcp://localhost:1883'
  ```

  - You can run `python simulators/iot/pumpjack/sim.py --help` for more details/options
