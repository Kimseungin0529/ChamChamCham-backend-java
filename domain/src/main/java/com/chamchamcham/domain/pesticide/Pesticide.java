package com.chamchamcham.domain.pesticide;

import com.chamchamcham.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "pesticide",
    uniqueConstraints = {
    @UniqueConstraint(name = "uk_pesticide_item_brand", columnNames = {"item_name", "brand_name"})
    }
)
public class Pesticide extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "item_name", nullable = false, length = 128)
    private String itemName;

    @Column(name = "brand_name", nullable = false, length = 128)
    private String brandName;

    @Column(name = "active_ingredient", length = 128)
    private String activeIngredient;

    @Column(length = 64)
    private String formulation;

    @Column(name = "usage_category", length = 32)
    private String usageCategory;

    @Column(name = "human_toxicity", length = 32)
    private String humanToxicity;

    @Column(name = "fish_toxicity", length = 32)
    private String fishToxicity;

    @Column(length = 128)
    private String manufacturer;

    protected Pesticide() {
    }

    public Pesticide(
        String itemName,
        String brandName
    ) {
        this(null, itemName, brandName, null, null, null, null, null, null);
    }

    public Pesticide(
        @Nullable UUID id,
        String itemName,
        String brandName
    ) {
        this(id, itemName, brandName, null, null, null, null, null, null);
    }

    public Pesticide(
        @Nullable UUID id,
        String itemName,
        String brandName,
        @Nullable String activeIngredient,
        @Nullable String formulation,
        @Nullable String usageCategory,
        @Nullable String humanToxicity,
        @Nullable String fishToxicity,
        @Nullable String manufacturer
    ) {
        this.id = id;
        this.itemName = Objects.requireNonNull(itemName, "itemName");
        this.brandName = Objects.requireNonNull(brandName, "brandName");
        this.activeIngredient = activeIngredient;
        this.formulation = formulation;
        this.usageCategory = usageCategory;
        this.humanToxicity = humanToxicity;
        this.fishToxicity = fishToxicity;
        this.manufacturer = manufacturer;
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = Objects.requireNonNull(itemName, "itemName");
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = Objects.requireNonNull(brandName, "brandName");
    }

    @Nullable
    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(@Nullable String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    @Nullable
    public String getFormulation() {
        return formulation;
    }

    public void setFormulation(@Nullable String formulation) {
        this.formulation = formulation;
    }

    @Nullable
    public String getUsageCategory() {
        return usageCategory;
    }

    public void setUsageCategory(@Nullable String usageCategory) {
        this.usageCategory = usageCategory;
    }

    @Nullable
    public String getHumanToxicity() {
        return humanToxicity;
    }

    public void setHumanToxicity(@Nullable String humanToxicity) {
        this.humanToxicity = humanToxicity;
    }

    @Nullable
    public String getFishToxicity() {
        return fishToxicity;
    }

    public void setFishToxicity(@Nullable String fishToxicity) {
        this.fishToxicity = fishToxicity;
    }

    @Nullable
    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(@Nullable String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void updateCatalogData(@Nullable String activeIngredient, @Nullable String formulation,
        @Nullable String usageCategory, @Nullable String humanToxicity, @Nullable String fishToxicity,
        @Nullable String manufacturer) {
        this.activeIngredient = activeIngredient;
        this.formulation = formulation;
        this.usageCategory = usageCategory;
        this.humanToxicity = humanToxicity;
        this.fishToxicity = fishToxicity;
        this.manufacturer = manufacturer;
    }
}
