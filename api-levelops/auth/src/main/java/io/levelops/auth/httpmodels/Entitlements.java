package io.levelops.auth.httpmodels;

import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;

import static io.levelops.auth.httpmodels.EntitlementsCategory.WRITE_CATEGORY;
import static io.levelops.auth.httpmodels.EntitlementsCategory.READ_CATEGORY;

@Getter
public enum Entitlements {

    ALL_FEATURES,
    INTEGRATIONS (WRITE_CATEGORY),
    DASHBOARDS (WRITE_CATEGORY),
    DASHBOARDS_READ (READ_CATEGORY),
    ISSUES (WRITE_CATEGORY),
    ISSUES_READ (READ_CATEGORY),
    PROPELS (WRITE_CATEGORY),
    PROPELS_READ (READ_CATEGORY),
    PROPELS_COUNT_5 (WRITE_CATEGORY),
    TRIAGE (WRITE_CATEGORY),
    TEMPLATES (WRITE_CATEGORY),
    REPORTS (WRITE_CATEGORY),
    TABLES (WRITE_CATEGORY),
    SETTINGS (WRITE_CATEGORY),
    SETTING_SCM_INTEGRATIONS_COUNT_3 (WRITE_CATEGORY);

    private EntitlementsCategory category;

    Entitlements() { }

    Entitlements(EntitlementsCategory category) {
        this.category = category;
    }

    public static Entitlements fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(Entitlements.class, value);
    }
}
