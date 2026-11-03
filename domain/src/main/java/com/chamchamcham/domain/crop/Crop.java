package com.chamchamcham.domain.crop;

import com.chamchamcham.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "crop",
    uniqueConstraints = {
    @UniqueConstraint(name = "uk_crop_external_no", columnNames = {"external_no"})
    }
)
public class Crop extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "external_no", nullable = false)
    private int externalNo;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_part_category", nullable = false, length = 64)
    private CropUsePartCategory usePartCategory;

    protected Crop() {
    }

    public Crop(
        int externalNo,
        String name,
        CropUsePartCategory usePartCategory
    ) {
        this(null, externalNo, name, usePartCategory);
    }

    public Crop(
        @Nullable UUID id,
        int externalNo,
        String name,
        CropUsePartCategory usePartCategory
    ) {
        this.id = id;
        this.externalNo = externalNo;
        this.name = Objects.requireNonNull(name, "name");
        this.usePartCategory = Objects.requireNonNull(usePartCategory, "usePartCategory");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public int getExternalNo() {
        return externalNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public CropUsePartCategory getUsePartCategory() {
        return usePartCategory;
    }

    public void setUsePartCategory(CropUsePartCategory usePartCategory) {
        this.usePartCategory = Objects.requireNonNull(usePartCategory, "usePartCategory");
    }

    public void updateCatalogData(String name, CropUsePartCategory usePartCategory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(usePartCategory, "usePartCategory");
        this.name = name;
        this.usePartCategory = usePartCategory;
    }
}
