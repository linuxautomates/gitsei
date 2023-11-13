package io.levelops.commons.report_models.conditions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.report_models.ESField;
import io.levelops.commons.report_models.conditions.WIBaseCondition;
import io.levelops.commons.report_models.conditions.LogicalCondition;
import io.levelops.commons.report_models.operators.ComparisionOperator;
import io.levelops.commons.report_models.operators.LogicalOperator;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class WIBaseConditionTest {
    @Test
    public void test() throws JsonProcessingException {
        WIBaseCondition bc1 = WIBaseCondition.builder()
                .field(ESField.w_components)
                .comparision(ComparisionOperator.IN)
                .values(List.of("component1", "component2"))
                .build();
        WIBaseCondition bc2 = WIBaseCondition.builder()
                .field(ESField.w_project)
                .comparision(ComparisionOperator.EQ)
                .values(List.of("SEI"))
                .build();

        WIBaseCondition bc3 = WIBaseCondition.builder()
                .field(ESField.w_components)
                .comparision(ComparisionOperator.IN)
                .values(List.of("component3", "component4"))
                .build();
        WIBaseCondition bc4 = WIBaseCondition.builder()
                .field(ESField.w_project)
                .comparision(ComparisionOperator.EQ)
                .values(List.of("PROP"))
                .build();

        LogicalCondition lc1 = LogicalCondition.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(bc1, bc2))
                .build();
        LogicalCondition lc2 = LogicalCondition.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(bc3, bc4))
                .build();
        LogicalCondition lc = LogicalCondition.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(lc1, lc2))
                .build();
        String str = DefaultObjectMapper.get().writeValueAsString(lc);
        Assert.assertNotNull(str);
    }

}