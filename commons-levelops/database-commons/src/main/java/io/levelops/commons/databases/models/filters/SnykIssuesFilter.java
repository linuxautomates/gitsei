package io.levelops.commons.databases.models.filters;

import com.google.common.base.MoreObjects;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class SnykIssuesFilter {

    List<String> orgs;
    List<String> projects;
    List<String> titles;
    List<String> types;
    List<String> severities;
    List<String> languages;
    List<String> versions;
    List<String> packageNames;
    List<String> cvssv3;
    List<String> packageManagers;
    List<String> exploitMaturities;
    String upgradable;
    String patchable;
    String pinnable;
    String ignored;
    String patched;
    List<String> integrationIds;
    Map<String, String> scoreRange;
    Map<String, String> disclosureDateRange;
    Map<String, String> publicationDateRange;

    Distinct across;
    Calculation calculation;

    public enum Distinct {
        org,
        project,
        title,
        type,
        severity,
        language,
        package_name,
        cvssv3,
        package_manager,
        exploit_maturity,
        upgradable,
        patchable,
        pinnable,
        ignored,
        patched,
        trend,
        none;

        public static Distinct fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(Distinct.class, st);
        }
    }

    public enum Calculation {
        total_issues,
        scores,
        total_patches;

        public static Calculation fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(Calculation.class, st);
        }
    }

    public Distinct getAcross() {
        Validate.notNull(across, "Across must be present for group by query");
        return across;
    }

    public Calculation getCalculation() {
        return MoreObjects.firstNonNull(calculation, Calculation.total_issues);
    }
}
