package com.chamchamcham.domain.farm;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.member.Member;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "farm")
public class Farm extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member owner;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "road_address", nullable = false, length = 255)
    private String roadAddress;

    @Column(name = "jibun_address", length = 255)
    private String jibunAddress;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 32)
    private String pnu;

    @Column(name = "land_category", length = 64)
    private String landCategory;

    @Column(name = "area_sqm", precision = 12, scale = 2)
    private BigDecimal areaSqm;

    @Column(name = "area_is_manual_entry", nullable = false)
    private boolean areaIsManualEntry;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "farm_boundary_coordinate", joinColumns = {@JoinColumn(name = "farm_id")})
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OrderColumn(name = "sequence")
    private List<FarmBoundaryCoordinate> boundaryCoordinates;

    @Embedded
    private FarmDataSource dataSource;

    protected Farm() {
    }

    public Farm(
        Member owner,
        String name,
        String roadAddress
    ) {
        this(null, owner, name, roadAddress, null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource());
    }

    public Farm(
        @Nullable UUID id,
        Member owner,
        String name,
        String roadAddress
    ) {
        this(id, owner, name, roadAddress, null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource());
    }

    public Farm(
        @Nullable UUID id,
        Member owner,
        String name,
        String roadAddress,
        @Nullable String jibunAddress,
        @Nullable Double latitude,
        @Nullable Double longitude,
        @Nullable String pnu,
        @Nullable String landCategory,
        @Nullable BigDecimal areaSqm,
        boolean areaIsManualEntry,
        List<FarmBoundaryCoordinate> boundaryCoordinates,
        FarmDataSource dataSource
    ) {
        this.id = id;
        this.owner = Objects.requireNonNull(owner, "owner");
        this.name = Objects.requireNonNull(name, "name");
        this.roadAddress = Objects.requireNonNull(roadAddress, "roadAddress");
        this.jibunAddress = jibunAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pnu = pnu;
        this.landCategory = landCategory;
        this.areaSqm = areaSqm;
        this.areaIsManualEntry = areaIsManualEntry;
        this.boundaryCoordinates = Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public Member getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public void setRoadAddress(String roadAddress) {
        this.roadAddress = Objects.requireNonNull(roadAddress, "roadAddress");
    }

    @Nullable
    public String getJibunAddress() {
        return jibunAddress;
    }

    public void setJibunAddress(@Nullable String jibunAddress) {
        this.jibunAddress = jibunAddress;
    }

    @Nullable
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(@Nullable Double latitude) {
        this.latitude = latitude;
    }

    @Nullable
    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(@Nullable Double longitude) {
        this.longitude = longitude;
    }

    @Nullable
    public String getPnu() {
        return pnu;
    }

    public void setPnu(@Nullable String pnu) {
        this.pnu = pnu;
    }

    @Nullable
    public String getLandCategory() {
        return landCategory;
    }

    public void setLandCategory(@Nullable String landCategory) {
        this.landCategory = landCategory;
    }

    @Nullable
    public BigDecimal getAreaSqm() {
        return areaSqm;
    }

    public void setAreaSqm(@Nullable BigDecimal areaSqm) {
        this.areaSqm = areaSqm;
    }

    public boolean getAreaIsManualEntry() {
        return areaIsManualEntry;
    }

    public void setAreaIsManualEntry(boolean areaIsManualEntry) {
        this.areaIsManualEntry = areaIsManualEntry;
    }

    public List<FarmBoundaryCoordinate> getBoundaryCoordinates() {
        return boundaryCoordinates;
    }

    public FarmDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(FarmDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public void updateProfile(String name, String roadAddress, @Nullable String jibunAddress,
        @Nullable Double latitude, @Nullable Double longitude, @Nullable String pnu, @Nullable String landCategory,
        @Nullable BigDecimal areaSqm, boolean areaIsManualEntry, List<FarmBoundaryCoordinate> boundaryCoordinates,
        FarmDataSource dataSource) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(roadAddress, "roadAddress");
        Objects.requireNonNull(boundaryCoordinates, "boundaryCoordinates");
        Objects.requireNonNull(dataSource, "dataSource");
        this.name = name;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pnu = pnu;
        this.landCategory = landCategory;
        this.areaSqm = areaSqm;
        this.areaIsManualEntry = areaIsManualEntry;
        this.boundaryCoordinates.clear();
        this.boundaryCoordinates.addAll(boundaryCoordinates);
        this.dataSource = dataSource;
    }
}
