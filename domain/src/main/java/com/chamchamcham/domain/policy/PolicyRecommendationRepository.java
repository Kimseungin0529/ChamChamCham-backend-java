package com.chamchamcham.domain.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PolicyRecommendationRepository extends JpaRepository<PolicyRecommendation, UUID> {
    @Query("""
        select r.policyProgram.id
        from PolicyRecommendation r
        where r.member.id = :memberId
          and r.policyProgram.source = :source
          and r.policyProgram.sourceYear = :sourceYear
        """)
    @NonNull
    List<UUID> findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(
        @Param("memberId") @NonNull UUID memberId,
        @Param("source") @NonNull PolicySource source,
        @Param("sourceYear") @NonNull String sourceYear
    );

    @Query("""
        select max(r.createdAt)
        from PolicyRecommendation r
        where r.member.id = :memberId
          and r.policyProgram.source = :source
          and r.policyProgram.sourceYear = :sourceYear
        """)
    @Nullable
    LocalDateTime findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(
        @Param("memberId") @NonNull UUID memberId,
        @Param("source") @NonNull PolicySource source,
        @Param("sourceYear") @NonNull String sourceYear
    );

    @Modifying
    @Query("""
        delete from PolicyRecommendation r
        where r.member.id = :memberId
          and r.policyProgram.id in (
              select p.id
              from PolicyProgram p
              where p.source = :source
                and p.sourceYear = :sourceYear
          )
        """)
    int deleteByMemberIdAndPolicyProgramSourceAndSourceYear(
        @Param("memberId") @NonNull UUID memberId,
        @Param("source") @NonNull PolicySource source,
        @Param("sourceYear") @NonNull String sourceYear
    );
}
