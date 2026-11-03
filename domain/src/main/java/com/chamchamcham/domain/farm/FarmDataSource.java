package com.chamchamcham.domain.farm;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.lang.Nullable;

@Embeddable
public class FarmDataSource {
    @Column(name = "data_source_address", length = 64)
    private String address;

    @Column(name = "data_source_coordinate", length = 64)
    private String coordinate;

    @Column(name = "data_source_parcel", length = 64)
    private String parcel;

    @Column(name = "data_source_land_characteristic", length = 64)
    private String landCharacteristic;

    public FarmDataSource() {
        this(null, null, null, null);
    }

    public FarmDataSource(
        @Nullable String address,
        @Nullable String coordinate,
        @Nullable String parcel,
        @Nullable String landCharacteristic
    ) {
        this.address = address;
        this.coordinate = coordinate;
        this.parcel = parcel;
        this.landCharacteristic = landCharacteristic;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    @Nullable
    public String getCoordinate() {
        return coordinate;
    }

    @Nullable
    public String getParcel() {
        return parcel;
    }

    @Nullable
    public String getLandCharacteristic() {
        return landCharacteristic;
    }
}
