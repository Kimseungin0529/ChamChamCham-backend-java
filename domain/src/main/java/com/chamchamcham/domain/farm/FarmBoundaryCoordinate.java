package com.chamchamcham.domain.farm;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.lang.Nullable;

@Embeddable
public class FarmBoundaryCoordinate {
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public FarmBoundaryCoordinate() {
        this(null, null);
    }

    public FarmBoundaryCoordinate(
        @Nullable Double latitude,
        @Nullable Double longitude
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Nullable
    public Double getLatitude() {
        return latitude;
    }

    @Nullable
    public Double getLongitude() {
        return longitude;
    }
}
