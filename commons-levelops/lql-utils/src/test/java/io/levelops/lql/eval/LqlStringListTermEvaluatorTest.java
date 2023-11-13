package io.levelops.lql.eval;

import io.levelops.lql.LQL;
import io.levelops.lql.exceptions.LqlException;
import org.assertj.core.util.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LqlStringListTermEvaluatorTest {

    @Test
    public void eqNull() throws LqlException {
        LqlStringListTermEvaluator evaluator = LqlStringListTermEvaluator.builder()
                .assignment("x", null)
                .build();
        assertThat(evaluator.evaluate(LQL.parse("x = \"a\""))).isEqualTo(false);
    }

    @Test
    public void eq() throws LqlException {
        LqlStringListTermEvaluator evaluator = LqlStringListTermEvaluator.builder()
                .assignment("x", Lists.newArrayList("a", null, "b", "c"))
                .build();
        assertThat(evaluator.evaluate(LQL.parse("x = \"a\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x = \"A\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x = \"b\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x = \"c\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x = \"d\""))).isEqualTo(false);
    }


    @Test
    public void neq() throws LqlException {
        LqlStringListTermEvaluator evaluator = LqlStringListTermEvaluator.builder()
                .assignment("x", Lists.newArrayList("a", "b", "c"))
                .build();
        assertThat(evaluator.evaluate(LQL.parse("x != \"a\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x != \"b\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x != \"c\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x != \"d\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x != \"D\""))).isEqualTo(true);
    }

    @Test
    public void contains() throws LqlException {
        LqlStringListTermEvaluator evaluator = LqlStringListTermEvaluator.builder()
                .assignment("x", Lists.newArrayList("a1", "b1", "c1"))
                .build();
        assertThat(evaluator.evaluate(LQL.parse("x ~ \"1\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x ~ \"b\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x ~ \"B\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x ~ \"d\""))).isEqualTo(false);
    }

    @Test
    public void ncontains() throws LqlException {
        LqlStringListTermEvaluator evaluator = LqlStringListTermEvaluator.builder()
                .assignment("x", Lists.newArrayList("a1", "b1", "c1"))
                .build();
        assertThat(evaluator.evaluate(LQL.parse("x !~ \"1\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x !~ \"b\""))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x !~ \"d\""))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x !~ \"D\""))).isEqualTo(true);
    }

    @Test
    public void in() throws LqlException {
        LqlStringListTermEvaluator evaluator = LqlStringListTermEvaluator.builder()
                .assignment("x", Lists.newArrayList("a", "B", "c"))
                .build();
        assertThat(evaluator.evaluate(LQL.parse("x in [ \"a\", \"b\" ]"))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x in [ \"c\" ]"))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x in [ \"d\" ]"))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x in [ \"a\", \"d\" ]"))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x in [ \"A\", \"D\" ]"))).isEqualTo(true);
    }

    @Test
    public void nin() throws LqlException {
        LqlStringListTermEvaluator evaluator = LqlStringListTermEvaluator.builder()
                .assignment("x", Lists.newArrayList("a", "B", "c"))
                .build();
        assertThat(evaluator.evaluate(LQL.parse("x nin [ \"a\", \"b\" ]"))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x nin [ \"c\" ]"))).isEqualTo(false);
        assertThat(evaluator.evaluate(LQL.parse("x nin [ \"d\" ]"))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x nin [ \"D\" ]"))).isEqualTo(true);
        assertThat(evaluator.evaluate(LQL.parse("x nin [ \"a\", \"d\" ]"))).isEqualTo(false);
    }
}