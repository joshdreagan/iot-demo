import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge

registry = new CollectorRegistry()

tachometerRpmGuage = Gauge.build().name("pumpjack_tachometer_rpm").help("RPM reading of the tachometer.").register(registry)
piezoVibrationFrequencyGuage = Gauge.build().name("pumpjack_piezo_vibration_hz").help("Frequency (in Hz) reading of the piezo vibration sensor.").register(registry)

request.body?.each() { body ->
    data = body['data']
    switch(body['type']) {
        case 'heartbeat':
            break
        case 'tachometer':
            tachometerRpmGuage.set(data['rpm'])
            break
        case 'piezo':
            piezoVibrationFrequencyGuage.set(data['vibrationFrequency'])
            break
        default:
            throw new IllegalArgumentException("Unknown metric type [${body['type']}]")
    }
}

return [registry, exchange.properties['PrometheusJobId'], ['instance': exchange.properties['PrometheusInstanceId']]]