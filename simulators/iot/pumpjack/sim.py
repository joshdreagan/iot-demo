import click
import json
import paho.mqtt.publish as publish
import random
import shutil
import time
from urllib.parse import urlparse

now = lambda: int(round(time.time() * 1000))

def __msg(topic, payload, qos=0, retain=False):
    return { "topic": topic, "payload": payload, "qos": qos, "retain": retain }

def __payload(location_id, rig_id, time, type, data=None):
    return { "locationId": location_id, "rigId": rig_id, "time": time, "source": "pumpjack", "type": type, "data": (data if data!=None else {}) }

def __heartbeat(location_id, rig_id, time):
    data = None
    return __payload(location_id, rig_id, time, "heartbeat", data)

def __tachometer(location_id, rig_id, time, speed, speed_variance):
    data = { "rpm": random.uniform(speed - speed_variance, speed + speed_variance) }
    return __payload(location_id, rig_id, time, "tachometer", data)

def __piezo(location_id, rig_id, time, frequency, frequency_variance):
    data = { "vibrationFrequency": random.uniform(frequency - frequency_variance, frequency + frequency_variance) }
    return __payload(location_id, rig_id, time, "piezo", data)

@click.command(context_settings={ "max_content_width": shutil.get_terminal_size()[0] })
@click.option("--location-id", help="The unique identifier for the location.")
@click.option("--rig-id", help="The unique identifier for the rig.")
@click.option("--broker-username", help="The username for the MQTT broker.")
@click.option("--broker-password", hide_input=True, prompt=True, confirmation_prompt=True, help="The password for the MQTT broker.")
@click.option("--telemetry-topic", default="iot.telemetry", show_default=True, help="The topic to send the telemetry data to.")
@click.option("--telemetry-frequency", type=click.IntRange(min=1, max=None), default=5, show_default=True, help="The frequency (in seconds) of the telemetry messages.")
@click.option("--buffer-timeout", type=click.IntRange(min=1000, max=None), default=10000, show_default=True, help="The time (in millis) to wait before sending each batch of messages to the MQTT broker.")
@click.option("--tachometer-enabled", is_flag=True, default=True, show_default=True, help="Whether or not to send tachometer sensor telemetry messages.")
@click.option("--tachometer-rotation-speed", type=click.FloatRange(min=0, max=None), default=12.0, show_default=True, help="The rotations per minute for the tachometer.")
@click.option("--tachometer-rotation-speed-variance", type=click.FloatRange(min=0, max=None), default=1.0, show_default=True, help="The variance of the rotations per minute for the tachometer.")
@click.option("--piezo-enabled", is_flag=True, default=True, show_default=True, help="Whether or not to send piezoo sensor telemetry messages.")
@click.option("--piezo-vibration-frequency", type=click.FloatRange(min=0.0, max=None), default=1000.0, show_default=True, help="The frequence (in Hz) of the vibrations for the piezo sensor.")
@click.option("--piezo-vibration-frequency-variance", type=click.FloatRange(min=0.0, max=None), default=1.0, show_default=True, help="The variance (in Hz) of the vibrations for the piezo sensor.")
@click.option("--verbose", is_flag=True, default=False, show_default=True, help="Enable verbose logging output")
@click.argument("broker_url", required=True)
def main(location_id, rig_id, broker_username, broker_password, telemetry_topic, telemetry_frequency, buffer_timeout, tachometer_enabled, tachometer_rotation_speed, tachometer_rotation_speed_variance, piezo_enabled, piezo_vibration_frequency, piezo_vibration_frequency_variance, verbose, broker_url):
    broker_url_parts = urlparse(broker_url)
    broker_auth = { "username": broker_username, "password": broker_password }
    last_run = now()
    msgs = list()
    while True:
        current_run = now()
        if verbose:
            click.echo("Woke up. Gathering telemetry data...")
            click.echo("Last run: {}, Current Run: {}".format(last_run, current_run))
        msg = __msg(telemetry_topic, json.dumps(__heartbeat(location_id, rig_id, current_run)))
        if verbose:
            click.echo(msg)
        msgs.append(msg)
        if tachometer_enabled:
            msg = __msg(telemetry_topic, json.dumps(__tachometer(location_id, rig_id, current_run, tachometer_rotation_speed, tachometer_rotation_speed_variance)))
            if verbose:
                click.echo(msg)
            msgs.append(msg)
        if piezo_enabled:
            msg = __msg(telemetry_topic, json.dumps(__piezo(location_id, rig_id, current_run, piezo_vibration_frequency, piezo_vibration_frequency_variance)))
            if verbose:
                click.echo(msg)
            msgs.append(msg)
        if current_run >= (last_run + buffer_timeout):
            click.echo("Publishing {} messages to {}:{}...".format(len(msgs), broker_url_parts.hostname, broker_url_parts.port))
            publish.multiple(msgs, hostname=broker_url_parts.hostname, port=broker_url_parts.port, auth=broker_auth, client_id="{}-{}".format(location_id, rig_id))
            click.echo("Done.")
            last_run = now()
            msgs.clear()
        time.sleep(telemetry_frequency)

if __name__ == "__main__":
    main()