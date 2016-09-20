package marchandivan.RoomMonitoring.sensor;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ivan on 9/5/16.
 */
public class SensorFactory {
    static private HashMap<String, Sensor> sensorMap = new HashMap<>();

    public static void Register(Sensor sensor) {
        sensorMap.put(sensor.getClass().getSimpleName(), sensor);
    }

    static {
        Register(new RaspberryPi());
        Register(new SensiThermostat());
    }

    static public ArrayList<String> GetTypes() {
        return new ArrayList<String>(sensorMap.keySet());
    }

    static public Sensor Get(String type) {
        try {
            if (sensorMap.containsKey(type)) {
                return sensorMap.get(type);
            }
        } catch (Exception e) {
        }
        return null;
    }

}
