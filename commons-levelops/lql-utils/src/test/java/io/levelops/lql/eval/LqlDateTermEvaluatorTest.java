package io.levelops.lql.eval;

import io.levelops.lql.LQL;
import io.levelops.lql.exceptions.LqlException;
import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class LqlDateTermEvaluatorTest {

    @Test
    public void testNull() throws LqlException {
        LqlDateTermEvaluator evaluator = LqlDateTermEvaluator.builder()
                .assignment("x", null)
                .build();

        assertThat(evaluator.evaluate(LQL.parse("x = \"2020-02-15T10:11:12Z\""))).isEqualTo(false);
    }

    @Test
    public void eq() throws LqlException {
        // "yyyy-MM-dd'T'HH:mm:ssZZ";
        LqlDateTermEvaluator evaluator = LqlDateTermEvaluator.builder()
                .assignment("x", Instant.parse("2020-02-15T10:11:12Z"))
                .build();

        assertThat(evaluator.evaluate(LQL.parse("x = \"2020-02-15T10:11:12Z\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x = \"9999-02-15T10:11:12Z\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x != \"2020-02-15T10:11:12Z\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x < \"2000-02-15T10:11:12Z\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x < \"2000-02-15T10:11:12Z\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x > \"2000-02-15T10:11:12Z\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x > \"2100-02-15T10:11:12Z\""))).isEqualTo(false);

        assertThat(evaluator.evaluate(LQL.parse("x <= \"2000-02-15T10:11:12Z\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x <= \"2000-02-15T10:11:12Z\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x >= \"2000-02-15T10:11:12Z\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x >= \"2100-02-15T10:11:12Z\""))).isEqualTo(false);
    }

}