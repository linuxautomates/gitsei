package io.levelops.lql.eval;

import io.levelops.lql.LQL;
import io.levelops.lql.exceptions.LqlException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LqlStringTermEvaluatorTest {


    @Test
    public void eq() throws LqlException {
        // "yyyy-MM-dd'T'HH:mm:ssZZ";
        LqlStringTermEvaluator evaluator = LqlStringTermEvaluator.builder()
                .assignment("x", "AbC")
                .build();

        assertThat(evaluator.evaluate(LQL.parse("x = \"abc\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x = \"aBc\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x = \"aaaaaaaa\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x != \"abc\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x in [\"abc\"]"))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x in [\"ABC\"]"))).isEqualTo(true);
    }


}