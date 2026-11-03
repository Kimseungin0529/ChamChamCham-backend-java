package com.chamchamcham.domain.crop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MemberCropRepository extends JpaRepository<MemberCrop, UUID> {
    long countByMemberId(@Param("memberId") @NonNull UUID memberId);

    @NonNull
    List<MemberCrop> findByMemberId(@Param("memberId") @NonNull UUID memberId);

    boolean existsByMemberIdAndFarmIdAndCropId(@Param("memberId") @NonNull UUID memberId, @Param("farmId") @NonNull UUID farmId, @Param("cropId") @NonNull UUID cropId);

    boolean existsByMemberIdAndFarmIdInAndCropId(@Param("memberId") @NonNull UUID memberId, @Param("farmIds") @NonNull Set<UUID> farmIds, @Param("cropId") @NonNull UUID cropId);

    @Query("""
        select mc
        from MemberCrop mc
        join fetch mc.crop
        where mc.member.id = :memberId
        """)
    @NonNull
    List<MemberCrop> findAllWithCropByMemberId(@Param("memberId") @NonNull UUID memberId);

    @Query("""
        select mc
        from MemberCrop mc
        join fetch mc.crop
        where mc.member.id = :memberId
          and mc.farm.id = :farmId
        """)
    @NonNull
    List<MemberCrop> findAllWithCropByMemberIdAndFarmId(@Param("memberId") @NonNull UUID memberId, @Param("farmId") @NonNull UUID farmId);

    void deleteByMemberIdAndFarmId(@Param("memberId") @NonNull UUID memberId, @Param("farmId") @NonNull UUID farmId);
}
