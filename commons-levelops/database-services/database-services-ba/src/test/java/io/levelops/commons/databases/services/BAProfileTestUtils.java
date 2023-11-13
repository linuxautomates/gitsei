package io.levelops.commons.databases.services;

import io.levelops.commons.report_models.ESField;
import io.levelops.commons.report_models.ba.BACategory;
import io.levelops.commons.report_models.ba.BAProfile;
import io.levelops.commons.report_models.conditions.LogicalCondition;
import io.levelops.commons.report_models.conditions.WIBaseCondition;
import io.levelops.commons.report_models.filters.BACategoryFilter;
import io.levelops.commons.report_models.filters.BAInheritanceFilter;
import io.levelops.commons.report_models.filters.BANonInheritanceFilter;
import io.levelops.commons.report_models.inheritance.WIInheritance;
import io.levelops.commons.report_models.operators.ComparisionOperator;
import io.levelops.commons.report_models.operators.LogicalOperator;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.UUID;

public class BAProfileTestUtils {
    private static BACategoryFilter buildCategoryFilter() {
        WIBaseCondition bc1 = WIBaseCondition.builder()
                .field(ESField.w_components)
                .comparision(ComparisionOperator.IN)
                .values(List.of("component1", "component2"))
                .build();
        WIBaseCondition bc2 = WIBaseCondition.builder()
                .field(ESField.w_project)
                .comparision(ComparisionOperator.EQ)
                .strValue("SEI")
                .build();
        WIBaseCondition bc3 = WIBaseCondition.builder()
                .field(ESField.w_custom_field)
                .customFieldName("customfield_2005")
                .comparision(ComparisionOperator.IN)
                .values(List.of("New", "Features"))
                .build();

        WIBaseCondition bc4 = WIBaseCondition.builder()
                .field(ESField.w_components)
                .comparision(ComparisionOperator.IN)
                .values(List.of("component3", "component4"))
                .build();
        WIBaseCondition bc5 = WIBaseCondition.builder()
                .field(ESField.w_project)
                .comparision(ComparisionOperator.EQ)
                .strValue("PROP")
                .build();

        LogicalCondition lc1 = LogicalCondition.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(bc1, bc2, bc3))
                .build();
        LogicalCondition lc2 = LogicalCondition.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(bc3, bc4, bc5))
                .build();
        LogicalCondition lc = LogicalCondition.builder()
                .operator(LogicalOperator.OR)
                .conditions(List.of(lc1, lc2))
                .build();

        BAInheritanceFilter inclusionFilter = BAInheritanceFilter.builder()
                .condition(lc)
                .inheritance(WIInheritance.ALL_TICKETS_AND_ALL_RECURSIVE_CHILDREN)
                .build();

        BANonInheritanceFilter exclusionFilter = BANonInheritanceFilter.builder()
                .condition(LogicalCondition.builder()
                        .operator(LogicalOperator.AND)
                        .conditions(List.of(
                                WIBaseCondition.builder()
                                        .field(ESField.w_workitem_type)
                                        .comparision(ComparisionOperator.IN)
                                        .values(List.of("Task", "Question"))
                                        .build()
                        ))
                        .build())
                .build();
        return BACategoryFilter.builder().inheritanceFilter(inclusionFilter).nonInheritanceFilter(exclusionFilter).excludeNonInheritance(true).build();
    }
    private static BACategory buildCategory(String categoryName, int index, String color, List<BACategory> subCategories) {
        BACategory.BACategoryBuilder bldr = BACategory.builder()
                .id(UUID.randomUUID())
                .name(categoryName).description(categoryName + " description").color(color)
                .index(index);
        if (CollectionUtils.isNotEmpty(subCategories)) {
            bldr.subCategories(subCategories);
        } else {
            bldr.categoryFilter(buildCategoryFilter());
        }
        return bldr.build();
    }

    public static BAProfile buildProfile(int i, boolean defaultProfile) {
        BACategory newFeatures = buildCategory("New", 0, "red", null);
        BACategory ktloBugs = buildCategory("KTLO - Bugs", 0, "blue", null);
        BACategory ktloFeatures = buildCategory("KTLO - Features", 1, "green", null);
        BACategory ktlo = buildCategory("KTLO", 1, "yellow", List.of(ktloBugs, ktloFeatures));

        BAProfile baProfile = BAProfile.builder()
                .id(UUID.randomUUID())
                .defaultProfile(defaultProfile)
                .name("Work Items " + i).description("BA Profile for Work Items " + i)
                .categories(List.of(newFeatures, ktlo))
                .build();
        return baProfile;
    }
}
