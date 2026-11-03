package com.chamchamcham.domain.pesticide;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

public interface PesticideRepository extends JpaRepository<Pesticide, UUID> {
    @Nullable
    Pesticide findByItemNameAndBrandName(@Param("itemName") @NonNull String itemName, @Param("brandName") @NonNull String brandName);
}
