package com.chamchamcham.application.policy.recommendation;

import com.chamchamcham.domain.member.ManagementType;
import java.time.LocalDate;
import java.util.Set;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record PolicyMemberProfile(
    @Nullable LocalDate birthDate,
    @Nullable Integer experienceLevel,
    @Nullable ManagementType managementType,
    @NonNull Set<String> cropNames,
    @NonNull Set<String> cropUsePartCategories,
    @NonNull Set<String> farmRegionTokens
) {
    public PolicyMemberProfile {
        Objects.requireNonNull(cropNames, "cropNames");
        Objects.requireNonNull(cropUsePartCategories, "cropUsePartCategories");
        Objects.requireNonNull(farmRegionTokens, "farmRegionTokens");
    }

    @Nullable
    public LocalDate getBirthDate() { return birthDate; }
    @Nullable
    public Integer getExperienceLevel() { return experienceLevel; }
    @Nullable
    public ManagementType getManagementType() { return managementType; }
    @NonNull
    public Set<String> getCropNames() { return cropNames; }
    @NonNull
    public Set<String> getCropUsePartCategories() { return cropUsePartCategories; }
    @NonNull
    public Set<String> getFarmRegionTokens() { return farmRegionTokens; }
}
