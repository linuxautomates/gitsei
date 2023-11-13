package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class SnykDepGraphTest {
    @Test
    public void testSerialize1() throws JsonProcessingException {
        SnykDepGraph.PkgManager pkgManager = SnykDepGraph.PkgManager.builder()
                .name("maven").build();
        List<SnykDepGraph.Pkg> pkgs = new ArrayList<>();
        pkgs.add(SnykDepGraph.Pkg.builder().id("com.wordnik:swagger-codegen_2.9.1@2.0.0").info(SnykDepGraph.PkgInfo.builder().name("com.wordnik:swagger-codegen_2.9.1").version("2.0.0").build()).build());
        pkgs.add(SnykDepGraph.Pkg.builder().id("com.fasterxml.jackson.core:jackson-annotations@2.10.1").info(SnykDepGraph.PkgInfo.builder().name("com.fasterxml.jackson.core:jackson-annotations").version("2.10.1").build()).build());

        List<SnykDepGraph.NodeDeps> deps = new ArrayList<>();
        deps.add(SnykDepGraph.NodeDeps.builder().nodeId("com.fasterxml.jackson.core:jackson-annotations@2.10.1").build());
        deps.add(SnykDepGraph.NodeDeps.builder().nodeId("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider@2.10.1").build());
        deps.add(SnykDepGraph.NodeDeps.builder().nodeId("com.fasterxml.jackson.module:jackson-module-scala@2.1.2").build());
        deps.add(SnykDepGraph.NodeDeps.builder().nodeId("commons-io:commons-io@2.3").build());

        SnykDepGraph.Node node1 = SnykDepGraph.Node.builder()
                .nodeId("root-node")
                .pkgId("com.wordnik:swagger-codegen_2.9.1@2.0.0")
                .deps(deps).build();
        SnykDepGraph.Graph graph = SnykDepGraph.Graph.builder()
                .rootNodeId("root-node")
                .nodes(Arrays.asList(node1))
                .build();

        SnykDepGraph depGraph = SnykDepGraph.builder()
                .pkgManager(pkgManager)
                .schemaVersion("1.2.0")
                .graph(graph)
                .build();

        SnykDepGraphWrapper wrapper = SnykDepGraphWrapper.builder()
                .depGraph(depGraph)
                .build();

        ObjectMapper mapper = Utils.constructObjectMapper();
        String data = mapper.writeValueAsString(wrapper);
        Assert.assertNotNull(data);
    }

    @Test
    public void testDeserialize1() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("integrations/snyk/SnykApiDepGraph_1.json").toURI());
        String data = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        ObjectMapper mapper = Utils.constructObjectMapper();
        SnykDepGraphWrapper wrapper = mapper.readValue(testFile, SnykDepGraphWrapper.class);
        Assert.assertNotNull(wrapper);
        Assert.assertEquals("1.2.0", wrapper.getDepGraph().getSchemaVersion());
        Assert.assertEquals("maven", wrapper.getDepGraph().getPkgManager().getName());
        Assert.assertEquals(27, wrapper.getDepGraph().getPkgs().size());
        Assert.assertEquals(27, wrapper.getDepGraph().getGraph().getNodes().size());
    }

    @Test
    public void testDeserialize2() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("integrations/snyk/SnykApiDepGraph_2.json").toURI());
        String data = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        ObjectMapper mapper = Utils.constructObjectMapper();
        SnykDepGraphWrapper wrapper = mapper.readValue(testFile, SnykDepGraphWrapper.class);
        Assert.assertNotNull(wrapper);
        Assert.assertEquals("1.2.0", wrapper.getDepGraph().getSchemaVersion());
        Assert.assertEquals("maven", wrapper.getDepGraph().getPkgManager().getName());
        Assert.assertEquals(6, wrapper.getDepGraph().getPkgs().size());
        Assert.assertEquals(6, wrapper.getDepGraph().getGraph().getNodes().size());
    }

    @Test
    public void testDeserialize3() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("integrations/snyk/SnykApiDepGraph_3.json").toURI());
        String data = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        ObjectMapper mapper = Utils.constructObjectMapper();
        SnykDepGraphWrapper wrapper = mapper.readValue(testFile, SnykDepGraphWrapper.class);
        Assert.assertNotNull(wrapper);
        Assert.assertEquals("1.2.0", wrapper.getDepGraph().getSchemaVersion());
        Assert.assertEquals("maven", wrapper.getDepGraph().getPkgManager().getName());
        Assert.assertEquals(6, wrapper.getDepGraph().getPkgs().size());
        Assert.assertEquals(6, wrapper.getDepGraph().getGraph().getNodes().size());
    }

    @Test
    public void testDeserialize4() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("integrations/snyk/SnykApiDepGraph_4.json").toURI());
        String data = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        ObjectMapper mapper = Utils.constructObjectMapper();
        SnykDepGraphWrapper wrapper = mapper.readValue(testFile, SnykDepGraphWrapper.class);
        Assert.assertNotNull(wrapper);
        Assert.assertEquals("1.2.0", wrapper.getDepGraph().getSchemaVersion());
        Assert.assertEquals("maven", wrapper.getDepGraph().getPkgManager().getName());
        Assert.assertEquals(27, wrapper.getDepGraph().getPkgs().size());
        Assert.assertEquals(27, wrapper.getDepGraph().getGraph().getNodes().size());
    }
}