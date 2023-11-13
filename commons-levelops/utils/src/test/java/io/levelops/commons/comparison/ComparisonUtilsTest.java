package io.levelops.commons.comparison;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComparisonUtilsTest {

    static class Pojo {
        String field;

        public Pojo(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }

    @Test
    public void test() {
        Pojo a = new Pojo("a");
        Pojo b = new Pojo("b");
        Pojo nullPojo = new Pojo(null);
        assertThat(ComparisonUtils.hasChanged(a, b, Pojo::getField)).isTrue();
        assertThat(ComparisonUtils.hasChanged(a, a, Pojo::getField)).isFalse();
        assertThat(ComparisonUtils.hasChanged(a, nullPojo, Pojo::getField)).isFalse();
        assertThat(ComparisonUtils.hasChanged(nullPojo, a, Pojo::getField)).isTrue();
    }
}