package ch.ffhs.parkapp;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by beku on 31.10.2016.
 */
public class BeaconSignal  implements Comparable {

    private int minor;
    private double distance;

    private double rssi;

    public BeaconSignal(int minor, double distance, double rssi){
        this.minor = minor;
        this.distance = distance;
        this.rssi = rssi;
    }

    public int getMinor() {
        return minor;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public int compareTo(Object another) {
        return  getDistance() - ((BeaconSignal)another).getDistance() < 0 ? -1 : 1;
    }

    public JSONObject toJson() {
        JSONObject jObj = new JSONObject();
        try {
            jObj.put("minor", getMinor());
            jObj.put("distance", getDistance());
            jObj.put("rssi", getRssi());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  jObj;
    }

    public double getRssi() {
        return rssi;
    }
}
