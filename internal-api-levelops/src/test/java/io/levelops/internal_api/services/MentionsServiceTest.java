package io.levelops.internal_api.services;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MentionsServiceTest {
    
    @Test
    public void test() {
        var a = extractMentionsFromText("hello this is a test with mentions @[test1@test.test] would be the first. Then we can have @[test2@test.test] as a second mention but test@test.test is not a mention nor is @[] or @[test@test].");
        Assertions.assertThat(a).containsExactlyInAnyOrder("test1@test.test", "test2@test.test");

        a = extractMentionsFromText("hello this is a test with mentions @[test1@test] would be the first. mention nor is @[] or @[test@test].");
        Assertions.assertThat(a).hasSize(0);
    }

    static final Pattern mentionPattern = Pattern
        .compile("\\@\\[(?=[a-zA-Z0-9@._%+-]{6,254}\\b)([a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\\.){1,8}[a-zA-Z]{2,63}\\b)\\]");
    
    public Set<String> extractMentionsFromText(final String message) {
        return mentionPattern.matcher(message).results().map(r -> r.group(1)).collect(Collectors.toSet());
    }
}