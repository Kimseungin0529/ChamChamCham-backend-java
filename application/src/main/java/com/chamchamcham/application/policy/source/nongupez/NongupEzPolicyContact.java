package com.chamchamcham.application.policy.source.nongupez;

import org.springframework.lang.Nullable;

public record NongupEzPolicyContact(
    @Nullable String agencyName,
    @Nullable String departmentName,
    @Nullable String phoneNumber
) {
    @Nullable
    public String getAgencyName() { return agencyName; }
    @Nullable
    public String getDepartmentName() { return departmentName; }
    @Nullable
    public String getPhoneNumber() { return phoneNumber; }
}
