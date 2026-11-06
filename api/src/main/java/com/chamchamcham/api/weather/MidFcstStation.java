package com.chamchamcham.api.weather;

import java.util.Objects;

public record MidFcstStation(String taRegId, String name, double latitude, double longitude) {
    public MidFcstStation {
        Objects.requireNonNull(taRegId,"taRegId");
        Objects.requireNonNull(name,"name");
    }
    public String getTaRegId() { return taRegId; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
