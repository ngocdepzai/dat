package hc.manager.datapp.service;

import android.location.Location;

public class CLocation extends Location {
    private boolean bUseMetricUnits = false;

    public CLocation(String provider) {
        super(provider);
    }

    public CLocation(Location location) {
        this(location, true);
    }

    public CLocation(Location location, boolean bUseMetricUnits) {
        super(location);
        this.bUseMetricUnits = bUseMetricUnits;
    }

    public boolean isbUseMetricUnits() {
        return bUseMetricUnits;
    }

    public void setbUseMetricUnits(boolean bUseMetricUnits) {
        this.bUseMetricUnits = bUseMetricUnits;
    }

    @Override
    public float distanceTo(Location dest) {
        float nDistance = super.distanceTo(dest);
        if (!this.isbUseMetricUnits()) {
            // convert meters to feet
            nDistance = nDistance * 3.28083989501312f;
        }
        return nDistance;
    }

    @Override
    public double getLatitude() {
        return super.getLatitude();
    }

    @Override
    public double getAltitude() {
        double nAltitude = super.getAltitude();
        if (!this.isbUseMetricUnits()) {
            // convert meters to feet
            nAltitude = nAltitude * 3.28083989501312d;
        }
        return nAltitude;
    }

    @Override
    public float getSpeed() {
        float nSpeed = super.getSpeed() * 3.6f;
        if (!this.isbUseMetricUnits()) {
            // convert meters/second to to moles/hours
            nSpeed = super.getSpeed() * 2.23693629f;
        }
        return nSpeed;
    }

    @Override
    public float getAccuracy() {
        float nAccuracy = super.getAccuracy();
        if (!this.isbUseMetricUnits()) {
            // convert meters to feet
            nAccuracy = nAccuracy * 3.28083989501312f;
        }
        return nAccuracy;
    }
}
