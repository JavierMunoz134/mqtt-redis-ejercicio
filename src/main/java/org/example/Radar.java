package org.example;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import redis.clients.jedis.Jedis;

public class Radar {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String TOPIC = "car/speed";
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final String EXCESS_KEY_PREFIX = "EXCESS:";
    private static final String VEHICLES_GROUP_KEY = "VEHICULOS";

    public static void main(String[] args) {
        Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT);
        String clientId = "Radar-" + System.currentTimeMillis();
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient mqttClient = new MqttClient(BROKER, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
            mqttClient.subscribe(TOPIC, (topic, message) -> processSpeedMessage(jedis, message));

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private static void processSpeedMessage(Jedis jedis, MqttMessage message) {
        String[] parts = new String(message.getPayload()).split(":");
        String licensePlate = parts[0];
        int speed = Integer.parseInt(parts[1]);

        if (speed > 80) {
            String excessKey = EXCESS_KEY_PREFIX + "80:" + licensePlate;
            jedis.set(excessKey, String.valueOf(speed));
        } else {
            jedis.sadd(VEHICLES_GROUP_KEY, licensePlate);
        }
    }
}
