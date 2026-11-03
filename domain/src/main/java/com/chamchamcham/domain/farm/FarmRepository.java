package com.chamchamcham.domain.farm;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

public interface FarmRepository extends JpaRepository<Farm, UUID> {
    @NonNull
    List<Farm> findByOwnerId(@Param("ownerId") @NonNull UUID ownerId);

    @Query("""
        select distinct f
        from Farm f
        left join fetch f.boundaryCoordinates
        where f.owner.id = :ownerId
        """)
    @NonNull
    List<Farm> findAllWithBoundaryCoordinatesByOwnerId(@Param("ownerId") @NonNull UUID ownerId);

    @Nullable
    Farm findByIdAndOwnerId(@Param("id") @NonNull UUID id, @Param("ownerId") @NonNull UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select f from Farm f
        where f.id = :farmId
          and f.owner.id = :memberId
        """)
    @Nullable
    Farm findOwnedByIdForReportUpdate(@Param("farmId") @NonNull UUID farmId, @Param("memberId") @NonNull UUID memberId);

    @Nullable
    Farm findFirstByOwnerIdOrderByCreatedAtAsc(@Param("ownerId") @NonNull UUID ownerId);
}
