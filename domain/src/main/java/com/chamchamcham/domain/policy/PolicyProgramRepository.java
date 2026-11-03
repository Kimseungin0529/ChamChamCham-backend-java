package com.chamchamcham.domain.policy;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PolicyProgramRepository extends JpaRepository<PolicyProgram, UUID> {
    @Query("""
        select p
        from PolicyProgram p
        where p.source = :source
          and p.sourceYear = :sourceYear
          and p.detailSynced = true
          and p.recommendable = true
          and (p.applyEndsOn is null or p.applyEndsOn >= :today)
        """)
    @NonNull
    List<PolicyProgram> findRecommendableCandidates(
        @Param("source") @NonNull PolicySource source,
        @Param("sourceYear") @NonNull String sourceYear,
        @Param("today") @NonNull LocalDate today
    );

    @NonNull
    List<PolicyProgram> findBySourceAndSourceYear(
        @NonNull PolicySource source,
        @NonNull String sourceYear
    );

    @Nullable
    PolicyProgram findByIdAndDetailSyncedTrueAndRecommendableTrue(
        @NonNull UUID id
    );

    @Query("""
        select p
        from PolicyProgram p
        where lower(p.title) like lower(concat('%', :keyword, '%'))
        order by p.title asc
        """)
    @NonNull
    List<PolicyProgram> findTitleSuggestions(
        @Param("keyword") @NonNull String keyword,
        @NonNull Pageable pageable
    );

    @Query("""
        select p
        from PolicyProgram p
        where lower(p.agencyName) like lower(concat('%', :keyword, '%'))
        order by p.agencyName asc
        """)
    @NonNull
    List<PolicyProgram> findAgencySuggestions(
        @Param("keyword") @NonNull String keyword,
        @NonNull Pageable pageable
    );
}
