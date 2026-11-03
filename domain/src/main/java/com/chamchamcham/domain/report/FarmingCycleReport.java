package com.chamchamcham.domain.report;

import java.util.Objects;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.member.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "farming_cycle_report")
public class FarmingCycleReport extends BaseTimeEntity {
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
    @Column(nullable = false, length = 16)
    private FarmingCycleReportStatus status;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_basis", nullable = false, length = 48)
    private FarmingCycleStartBasis startBasis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_harvest_record_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private FarmingRecord finalHarvestRecord;

    @Column(name = "statistics_schema_version", nullable = false)
    private int statisticsSchemaVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private CycleReportStatistics statistics;

    @Column(name = "source_revision", nullable = false)
    private long sourceRevision;

    protected FarmingCycleReport() {}

    private FarmingCycleReport(UUID id, Member member, Farm farm, Crop crop, FarmingCycleReportStatus status,
                               LocalDateTime startsAt, LocalDateTime endsAt, FarmingCycleStartBasis startBasis,
                               FarmingRecord finalHarvestRecord, int statisticsSchemaVersion,
                               CycleReportStatistics statistics, long sourceRevision) {
        this.id = id;
        this.member = Objects.requireNonNull(member, "member");
        this.farm = Objects.requireNonNull(farm, "farm");
        this.crop = Objects.requireNonNull(crop, "crop");
        this.status = Objects.requireNonNull(status, "status");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt");
        this.endsAt = endsAt;
        this.startBasis = Objects.requireNonNull(startBasis, "startBasis");
        this.finalHarvestRecord = finalHarvestRecord;
        this.statisticsSchemaVersion = statisticsSchemaVersion;
        this.statistics = Objects.requireNonNull(statistics, "statistics");
        this.sourceRevision = sourceRevision;
    }

    public boolean applyProjection(FarmingCycleReportProjection next) {
        requireValid(next);
        if (isSameProjection(next)) return false;
        status = next.status();
        startsAt = next.startsAt();
        endsAt = next.endsAt();
        startBasis = next.startBasis();
        finalHarvestRecord = next.finalHarvestRecord();
        statisticsSchemaVersion = next.statisticsSchemaVersion();
        statistics = next.statistics();
        sourceRevision += 1;
        return true;
    }

    public boolean supersede() {
        if (status == FarmingCycleReportStatus.SUPERSEDED) return false;
        status = FarmingCycleReportStatus.SUPERSEDED;
        sourceRevision += 1;
        return true;
    }

    private boolean isSameProjection(FarmingCycleReportProjection next) {
        return status == next.status() && Objects.equals(startsAt, next.startsAt()) && Objects.equals(endsAt, next.endsAt())
            && startBasis == next.startBasis()
            && Objects.equals(finalHarvestRecord == null ? null : finalHarvestRecord.getId(), next.finalHarvestRecord() == null ? null : next.finalHarvestRecord().getId())
            && statisticsSchemaVersion == next.statisticsSchemaVersion() && Objects.equals(statistics, next.statistics());
    }

    public static FarmingCycleReport create(Member member, Farm farm, Crop crop, FarmingCycleReportProjection projection) {
        requireValid(projection);
        return new FarmingCycleReport(null, member, farm, crop, projection.status(), projection.startsAt(), projection.endsAt(),
            projection.startBasis(), projection.finalHarvestRecord(), projection.statisticsSchemaVersion(), projection.statistics(), 1);
    }

    private static void requireValid(FarmingCycleReportProjection projection) {
        if (projection.statisticsSchemaVersion() <= 0) throw new IllegalArgumentException("Failed requirement.");
        if (projection.endsAt() != null && projection.startsAt().isAfter(projection.endsAt())) throw new IllegalArgumentException("Failed requirement.");
        switch (projection.status()) {
            case ACTIVE -> {
                if (projection.endsAt() != null || projection.finalHarvestRecord() != null) throw new IllegalArgumentException("Failed requirement.");
            }
            case COMPLETED -> {
                if (projection.endsAt() == null || projection.finalHarvestRecord() == null) throw new IllegalArgumentException("Failed requirement.");
            }
            case SUPERSEDED -> throw new IllegalArgumentException("SUPERSEDED is not a source projection");
        }
    }

    public UUID getId() { return id; }
    public Member getMember() { return member; }
    public Farm getFarm() { return farm; }
    public Crop getCrop() { return crop; }
    public FarmingCycleReportStatus getStatus() { return status; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public FarmingCycleStartBasis getStartBasis() { return startBasis; }
    public FarmingRecord getFinalHarvestRecord() { return finalHarvestRecord; }
    public int getStatisticsSchemaVersion() { return statisticsSchemaVersion; }
    public CycleReportStatistics getStatistics() { return statistics; }
    public long getSourceRevision() { return sourceRevision; }
}
