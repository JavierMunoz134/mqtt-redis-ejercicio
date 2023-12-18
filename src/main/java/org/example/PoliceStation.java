package org.example;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import redis.clients.jedis.Jedis;

import java.util.Set;

public class PoliceStation {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String EXCESS_KEY_PREFIX = "EXCESS:";
    private static final String VEHICLES_GROUP_KEY = "VEHICULOS";
    private static final String DENOUNCED_VEHICLES_GROUP_KEY = "VEHICULOSDENUNCIADOS";

    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost", 6379);
        String clientId = "PoliceStation-" + System.currentTimeMillis();
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient mqttClient = new MqttClient(BROKER, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            mqttClient.connect(connOpts);
            mqttClient.setCallback(new PoliceStationCallback(jedis));

            mqttClient.subscribe(EXCESS_KEY_PREFIX + "#");
            Thread.sleep(1000);

            while (true) {
                printStatistics(jedis);
                Thread.sleep(1000);
            }

        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void printStatistics(Jedis jedis) {
        Set<String> allVehicles = jedis.smembers(VEHICLES_GROUP_KEY);
        Set<String> denouncedVehicles = jedis.smembers(DENOUNCED_VEHICLES_GROUP_KEY);

        int totalVehicles = allVehicles.size();
        int denouncedCount = denouncedVehicles.size();
        double denouncedPercentage = (double) denouncedCount / totalVehicles * 100;

        System.out.printf("Total de vehículos: %d, Vehículos denunciados: %d, Porcentaje denunciados: %.2f%%\n",
                totalVehicles, denouncedCount, denouncedPercentage);
    }
}

class PoliceStationCallback implements MqttCallback {

    private final Jedis jedis;

    public PoliceStationCallback(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Conexión perdida con el servidor MQTT.");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String excessKey = topic.substring(topic.indexOf(":") + 1);
        String licensePlate = excessKey.substring(excessKey.indexOf(":") + 1);

        int speed = Integer.parseInt(message.toString());
        int fineAmount = calculateFineAmount(speed);

        System.out.printf("¡Multa enviada a %s por exceso de velocidad! Importe: %d €\n", licensePlate, fineAmount);

        jedis.sadd("VEHICULOSDENUNCIADOS", licensePlate);
        jedis.del(excessKey);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No se utiliza en este ejemplo
    }

    private int calculateFineAmount(int speed) {
        if (speed > 80 && speed <= 88) {
            return 100;
        } else if (speed > 88 && speed <= 96) {
            return 200;
        } else if (speed > 96) {
            return 500;
        } else {
            return 0;
        }
    }
}

