package io.levelops.commons.regex;

import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

public class RegexServiceTest {
    RegexService service = new RegexService();

    @Test
    public void testApply() throws IOException {
        String txt = ResourceUtils.getResourceAsString("samples/error.log");
        Assertions.assertThat(service.findRegexHits(Set.of("[asd]{8}"), txt).getTotalMatches()).isEqualTo(1);
        Assertions.assertThat(service.findRegexHits(Set.of("(?i)regex"), txt).getFirstHitContext().length()).isGreaterThan(0);
        Assertions.assertThat(service.findRegexHits(Set.of("[asd]{8}"), txt).getFirstHitLineNumber()).isEqualTo(26);
        Assertions.assertThat(service.findRegexHits(Set.of("(?i)regex"), txt).getFirstHitLineNumber()).isGreaterThan(0);
        Assertions.assertThat(service.findRegexHits(Set.of("dsl\\.internal\\.WatchConnectionio"), txt).getTotalMatches()).isEqualTo(58);
        Assertions.assertThat(service.findRegexHits(Set.of("(?i)booboo"), txt).getFirstHitContext()).isEqualTo(null);
        Assertions.assertThat(service.findRegexHits(Set.of("(?i)dodo"), txt).getTotalMatches()).isEqualTo(0);
        txt = ResourceUtils.getResourceAsString("samples/error2.log");
        Assertions.assertThat(service.findRegexHits(Set.of("dsl\\.internal\\.WatchConnectionio"), txt)
                .getFirstHitLineNumber()).isEqualTo(1);
    }

}
