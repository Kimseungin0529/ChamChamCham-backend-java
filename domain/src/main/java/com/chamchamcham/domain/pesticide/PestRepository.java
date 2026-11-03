package com.chamchamcham.domain.pesticide;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

public interface PestRepository extends JpaRepository<Pest, UUID> {
    @Nullable
    Pest findByName(@Param("name") @NonNull String name);
}
