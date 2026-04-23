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