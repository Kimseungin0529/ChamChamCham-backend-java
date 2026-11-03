package com.chamchamcham.domain.pesticide;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

public interface PesticideApplicationRepository extends JpaRepository<PesticideApplication, UUID> {
    @Nullable
    PesticideApplication findByPesticide_IdAndPest_IdAndCropName(@Param("pesticideId") @NonNull UUID pesticideId, @Param("pestId") @NonNull UUID pestId, @Param("cropName") @NonNull String cropName);

    @Query("""
        select distinct pa.pest
        from PesticideApplication pa
        where pa.pesticide.id = :pesticideId
        order by pa.pest.name asc
        """)
    @NonNull
    List<Pest> findDistinctPestsByPesticideId(@Param("pesticideId") @NonNull UUID pesticideId);
}
