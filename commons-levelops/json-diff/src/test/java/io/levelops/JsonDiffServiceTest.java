package io.levelops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.models.JsonDiff;
import io.levelops.utils.JsonDiffUtils;
import io.levelops.utils.JsonDiffUtils.PathDiff;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonDiffServiceTest {
    private JsonDiffService jsonDiffService = new JsonDiffService(DefaultObjectMapper.get());

    @Test
    public void testNull() throws IOException {
        Map<String, JsonDiff> diff = jsonDiffService.diff(null, null, null);
        assertThat(diff).containsKey("/");
    }

    @Test
    public void testNull2() throws IOException {
        String jsonB = ResourceUtils.getResourceAsString("diff/jenkinsB.json");
        Map<String, JsonDiff> diff = jsonDiffService.diff(null, jsonB, null);
        DefaultObjectMapper.prettyPrint(diff);
        assertThat(diff).containsKey("/");
        assertThat(diff.get("/").getAdded()).containsExactlyInAnyOrder("/config", "/plugins", "/users");
    }

    @Test
    public void testJenkins() throws IOException {
        testDiff("diff/jenkinsA.json", "diff/jenkinsB.json", "diff/jenkinsDiff.json");
    }

    @Test
    public void testMap() throws IOException {
        testDiff("diff/mapA.json", "diff/mapB.json", "diff/mapDiff.json");
    }

    @Test
    public void testMapRemovedTopLevel() throws IOException {
//        testDiff("diff/mapA.json", "diff/emptyMap.json", "diff/emptyMapDiff.json");

        String jsonB = ResourceUtils.getResourceAsString("diff/mapA.json");
        JsonNode diff = JsonDiffUtils.diffAsJson(NullNode.getInstance(), DefaultObjectMapper.get().readTree(jsonB));
        System.out.println(diff.toString());
    }

    @Test
    public void testArray() throws IOException {
        testDiff("diff/arrayA.json", "diff/arrayB.json", "diff/arrayDiff.json");

        // NB: diff works but there is ambiguity when a item is removed next to an item that is modified
    }

    private void testDiff(String resourceA, String resourceB, String resourceDiff) throws IOException {
        String jsonA = ResourceUtils.getResourceAsString(resourceA);
        String jsonB = ResourceUtils.getResourceAsString(resourceB);
        String expected = ResourceUtils.getResourceAsString(resourceDiff);
        List<PathDiff> diff = JsonDiffUtils.diff(DefaultObjectMapper.get(), jsonA, jsonB);

        DefaultObjectMapper.prettyPrint(diff);

        assertThat(diff).usingFieldByFieldElementComparator().isEqualTo(DefaultObjectMapper.get().readValue(expected,
                DefaultObjectMapper.get().getTypeFactory().constructCollectionType(List.class, PathDiff.class)));
    }

    @Test
    public void test() throws IOException {
        testAnalyzeDiff("diff/mapA.json", "diff/mapB.json", "diff/mapDiff2.json",
                List.of("/plugins"));
    }

    @Test
    public void test2() throws IOException {
        testAnalyzeDiff("diff/jenkinsA.json", "diff/jenkinsB.json", "diff/jenkinsDiff2.json",
                List.of("/config", "/plugins", "/users"));
    }

    @Test
    public void testEscaping() throws IOException {
        testAnalyzeDiff("diff/escapeA.json", "diff/escapeB.json", "diff/escapeDiff.json",
                List.of("/plugins"));
    }

    @Test
    public void testRoot() throws IOException {
        testAnalyzeDiff("diff/rootA.json", "diff/rootB.json", "diff/rootDiff.json",
                List.of("/"));
    }


    @Test
    public void testTopLevel() throws IOException {
        // THIS WON'T RETURN ANYTHING CAUSE BASE IS MISSING
        testAnalyzeDiff("diff/topLevelAddedA.json", "diff/topLevelAddedB.json", "diff/topLevelDiff.json",
                List.of("/config"));
        testAnalyzeDiff("diff/topLevelRemovedA.json", "diff/topLevelRemovedB.json", "diff/topLevelDiff.json",
                List.of("/config"));
    }

    private void testAnalyzeDiff(String resourceA, String resourceB, String resourceExpected, List<String> basePathList) throws IOException {
        String beforeJson = ResourceUtils.getResourceAsString(resourceA);
        String afterJson = ResourceUtils.getResourceAsString(resourceB);
        String expected = ResourceUtils.getResourceAsString(resourceExpected);
        Map<String, JsonDiff> jsonDiffMap = jsonDiffService.diff(beforeJson, afterJson, basePathList);
        DefaultObjectMapper.prettyPrint(jsonDiffMap);
        assertThat(jsonDiffMap).isEqualTo(DefaultObjectMapper.get().readValue(expected,
                DefaultObjectMapper.get().getTypeFactory().constructMapType(Map.class, String.class, JsonDiff.class)));
    }

}