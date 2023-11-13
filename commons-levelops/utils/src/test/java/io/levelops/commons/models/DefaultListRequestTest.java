package io.levelops.commons.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultListRequestTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    @SuppressWarnings("unchecked")
    public void name() {
        DefaultListRequest request = DefaultListRequest.builder()
                .filter(Map.of(
                        "labels", List.of("a", "b", "c")
                ))
                .build();
        assertThat(request.getFilterValue("labels", List.class).orElse(null)).containsExactlyInAnyOrder("a", "b", "c");
        assertThat(request.getFilterValueAsList("labels").orElse(null)).containsExactlyInAnyOrder("a", "b", "c");
        assertThat(request.getFilterValueAsSet("labels").orElse(null)).containsExactlyInAnyOrder("a", "b", "c");
        assertThat(request.getFilterValue("labels", Object.class).map(o -> Set.copyOf((List<String>) o)).orElse(Set.of())).containsExactlyInAnyOrder("a", "b", "c");

        var request2 = DefaultListRequest.builder()
                .filter(Map.of(
                        "labels", Set.of("a", "b", "c")
                ))
                .build();
        assertThat(request2.getFilterValue("labels", Set.class).orElse(null)).containsExactlyInAnyOrder("a", "b", "c");
        assertThat(request2.getFilterValueAsList("labels").orElse(null)).containsExactlyInAnyOrder("a", "b", "c");
        assertThat(request2.getFilterValueAsSet("labels").orElse(null)).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    public void name2() {
        DefaultListRequest request = DefaultListRequest.builder()
                .pageSize(0)
                .page(0)
                .filter(Collections.emptyMap())
                .build();
        Boolean abc = request.getFilterValue("a", Boolean.class).orElse(null);
        Assert.assertNull(abc);

    }

    @Test
    public void testSerializtion() throws JsonProcessingException {
        DefaultListRequest request = DefaultListRequest.builder()
                .filter(Map.of(
                        "labels", List.of("a", "b", "c")
                ))
                .across("across")
                .stacks(List.of("stack1", "stack2"))
                .ouIds(Set.of(1,2,3))
                .ouUserFilterDesignation(Map.of("jira", Set.of("assignee")))
                .build();
        String serialized = MAPPER.writeValueAsString(request);
        DefaultListRequest actual = MAPPER.readValue(serialized, DefaultListRequest.class);
        Assert.assertEquals(actual, request);
    }

    @Test
    public void testDeserializtion() throws IOException {
        DefaultListRequest request = DefaultListRequest.builder()
                .filter(Map.of(
                        "labels", List.of("a", "b", "c")
                ))
                .across("across")
                .stacks(List.of("stack1", "stack2"))
                .ouIds(Set.of(1,2,3))
                .ouUserFilterDesignation(Map.of("jira", Set.of("assignee")))
                .build();
        String serialized = ResourceUtils.getResourceAsString("models/default_list_request.json");
        DefaultListRequest actual = MAPPER.readValue(serialized, DefaultListRequest.class);
        Assert.assertEquals(actual, request);
    }
}