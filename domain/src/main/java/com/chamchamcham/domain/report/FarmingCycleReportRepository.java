package com.chamchamcham.domain.report;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface FarmingCycleReportRepository extends JpaRepository<FarmingCycleReport, UUID> {
    FarmingCycleReport findByIdAndMember_Id(UUID id, UUID memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select report from FarmingCycleReport report
        where report.id = :reportId
          and report.member.id = :memberId
        """)
    FarmingCycleReport findByIdAndMemberIdForUpdate(@Param("reportId") UUID reportId, @Param("memberId") UUID memberId);

    FarmingCycleReport findByMember_IdAndFarm_IdAndCrop_IdAndStatus(UUID memberId, UUID farmId, UUID cropId, FarmingCycleReportStatus status);

    @Query("""
        select r from FarmingCycleReport r
        where r.member.id = :memberId
          and r.farm.id = :farmId
          and r.crop.id = :cropId
          and r.status in (
            com.chamchamcham.domain.report.FarmingCycleReportStatus.ACTIVE,
            com.chamchamcham.domain.report.FarmingCycleReportStatus.COMPLETED
          )
        order by r.startsAt asc, r.id asc
        """)
    List<FarmingCycleReport> findAllCurrent(@Param("memberId") UUID memberId, @Param("farmId") UUID farmId, @Param("cropId") UUID cropId);
}
