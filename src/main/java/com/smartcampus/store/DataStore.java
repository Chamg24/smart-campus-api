package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {

    private static final DataStore INSTANCE = new DataStore();
    public static DataStore getInstance() { return INSTANCE; }

    private final Map<String, Room>                rooms    = new ConcurrentHashMap<>();
    private final Map<String, Sensor>              sensors  = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        // Seed rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 40);
        Room r2 = new Room("LAB-102", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        // Seed sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE",    21.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001",  "CO2",         "ACTIVE",   420.0, "LAB-102");
        Sensor s3 = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", 0.0, "LIB-301");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        // Link sensors to rooms
        r1.getSensorIds().add("TEMP-001");
        r1.getSensorIds().add("OCC-001");
        r2.getSensorIds().add("CO2-001");

        // Seed readings
        readings.put("TEMP-001", new ArrayList<>());
        readings.get("TEMP-001").add(new SensorReading(21.5));
        readings.put("CO2-001",  new ArrayList<>());
        readings.put("OCC-001",  new ArrayList<>());
    }

    public Map<String, Room>    getRooms()              { return rooms; }
    public Room                 getRoom(String id)      { return rooms.get(id); }
    public void                 putRoom(Room r)         { rooms.put(r.getId(), r); }
    public Room                 removeRoom(String id)   { return rooms.remove(id); }

    public Map<String, Sensor>  getSensors()            { return sensors; }
    public Sensor               getSensor(String id)    { return sensors.get(id); }
    public void                 putSensor(Sensor s)     { sensors.put(s.getId(), s); }
    public Sensor               removeSensor(String id) { return sensors.remove(id); }

    public List<SensorReading> getReadings(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        List<SensorReading> list = readings.computeIfAbsent(sensorId, k -> new ArrayList<>());
        synchronized (list) {
            list.add(reading);
        }
    }
}