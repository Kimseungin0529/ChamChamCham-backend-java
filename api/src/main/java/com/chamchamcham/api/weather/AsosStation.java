package com.chamchamcham.api.weather;

import java.util.Objects;

public record AsosStation(String id, String name, double latitude, double longitude) {
    public AsosStation {
        Objects.requireNonNull(id,"id");
        Objects.requireNonNull(name,"name");
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
