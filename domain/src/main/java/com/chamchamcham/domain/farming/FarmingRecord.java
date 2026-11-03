package com.chamchamcham.domain.farming;

import java.util.Objects;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "farming_record")
public class FarmingRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 32)
    private WorkType workType;

    @Column(name = "worked_at", nullable = false)
    private LocalDateTime workedAt;

    @Column(name = "weather_condition", nullable = false, length = 64)
    private String weatherCondition;

    @Column(name = "weather_temperature", nullable = false)
    private int weatherTemperature;

    @Column(columnDefinition = "text", nullable = false)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_mode", nullable = false, length = 32)
    private EntryMode entryMode;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    protected FarmingRecord() {}

    public FarmingRecord(UUID id, Member member, Farm farm, Crop crop, WorkType workType, LocalDateTime workedAt, String weatherCondition, int weatherTemperature, String memo, EntryMode entryMode, boolean isDeleted) {
        this.id = id;
        this.member = Objects.requireNonNull(member, "member");
        this.farm = Objects.requireNonNull(farm, "farm");
        this.crop = Objects.requireNonNull(crop, "crop");
        this.workType = Objects.requireNonNull(workType, "workType");
        this.workedAt = Objects.requireNonNull(workedAt, "workedAt");
        this.weatherCondition = Objects.requireNonNull(weatherCondition, "weatherCondition");
        this.weatherTemperature = weatherTemperature;
        this.memo = Objects.requireNonNull(memo, "memo");
        this.entryMode = Objects.requireNonNull(entryMode, "entryMode");
        this.isDeleted = isDeleted;
    }

    public FarmingRecord(Member member, Farm farm, Crop crop, WorkType workType, LocalDateTime workedAt, String weatherCondition, int weatherTemperature, String memo, EntryMode entryMode) {
        this(null, member, farm, crop, workType, workedAt, weatherCondition, weatherTemperature, memo, entryMode, false);
    }

    public FarmingRecord(Member member, Farm farm, Crop crop, WorkType workType, LocalDateTime workedAt, String weatherCondition, int weatherTemperature, String memo, EntryMode entryMode, boolean isDeleted) {
        this(null, member, farm, crop, workType, workedAt, weatherCondition, weatherTemperature, memo, entryMode, isDeleted);
    }

    public UUID getId() { return id; }

    public Member getMember() { return member; }

    public Farm getFarm() { return farm; }

    public void setFarm(Farm farm) { this.farm = Objects.requireNonNull(farm, "farm"); }

    public Crop getCrop() { return crop; }

    public void setCrop(Crop crop) { this.crop = Objects.requireNonNull(crop, "crop"); }

    public WorkType getWorkType() { return workType; }

    public void setWorkType(WorkType workType) { this.workType = Objects.requireNonNull(workType, "workType"); }

    public LocalDateTime getWorkedAt() { return workedAt; }

    public void setWorkedAt(LocalDateTime workedAt) { this.workedAt = Objects.requireNonNull(workedAt, "workedAt"); }

    public String getWeatherCondition() { return weatherCondition; }

    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = Objects.requireNonNull(weatherCondition, "weatherCondition"); }

    public int getWeatherTemperature() { return weatherTemperature; }

    public void setWeatherTemperature(int weatherTemperature) { this.weatherTemperature = weatherTemperature; }

    public String getMemo() { return memo; }

    public void setMemo(String memo) { this.memo = Objects.requireNonNull(memo, "memo"); }

    public EntryMode getEntryMode() { return entryMode; }

    public boolean isDeleted() { return isDeleted; }

    public void setDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }

    public void update(Farm farm, Crop crop, WorkType workType, LocalDateTime workedAt, String weatherCondition, int weatherTemperature, String memo) {
        this.farm = Objects.requireNonNull(farm, "farm");
        this.crop = Objects.requireNonNull(crop, "crop");
        this.workType = Objects.requireNonNull(workType, "workType");
        this.workedAt = Objects.requireNonNull(workedAt, "workedAt");
        this.weatherCondition = Objects.requireNonNull(weatherCondition, "weatherCondition");
        this.weatherTemperature = weatherTemperature;
        this.memo = Objects.requireNonNull(memo, "memo");
    }

    public void softDelete() { isDeleted = true; }
}
