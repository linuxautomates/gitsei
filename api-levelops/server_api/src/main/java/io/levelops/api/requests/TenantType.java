package io.levelops.api.requests;

import lombok.Getter;

@Getter
public enum TenantType {
    TRIAL_TENANT("limited_trial"),
    FULL_TENANT("full");

    private final String licenseType;

    private TenantType(String licenseType) {
        this.licenseType = licenseType;
    }
}
