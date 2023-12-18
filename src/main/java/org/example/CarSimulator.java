package org.example;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

public class CarSimulator {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String TOPIC = "car/speed";
    private static final String[] LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    public static void main(String[] args) {
        String clientId = "CarSimulator-" + System.currentTimeMillis();
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient mqttClient = new MqttClient(BROKER, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);

            while (true) {
                String licensePlate = generateLicensePlate();
                int speed = generateSpeed();
                String message = licensePlate + ":" + speed;

                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttClient.publish(TOPIC, mqttMessage);

                Thread.sleep(1000);
            }

        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String generateLicensePlate() {
        Random random = new Random();
        return String.format("%04d%s%s%s", random.nextInt(10000),
                LETTERS[random.nextInt(LETTERS.length)],
                LETTERS[random.nextInt(LETTERS.length)],
                LETTERS[random.nextInt(LETTERS.length)]);
    }

    private static int generateSpeed() {
        Random random = new Random();
        return random.nextInt(81) + 60; // Velocidad entre 60 y 140 km/h
    }
}
