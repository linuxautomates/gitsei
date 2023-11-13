package io.levelops.commons.databases.models.database.scm;

import io.levelops.commons.databases.models.database.IntegrationConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepoConfigEntryMatcherTest {
    private static final boolean CASE_SENSITIVE = true;
    private static final List<IntegrationConfig.RepoConfigEntry> CONFIG_ENTRIES = List.of(
            IntegrationConfig.RepoConfigEntry.builder().repoId(null).pathPrefix(null).build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("").pathPrefix(null).build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId(null).pathPrefix("").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("").pathPrefix("").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("id1").pathPrefix("//depot/d1").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("id2").pathPrefix("//depot/d2").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("id3").pathPrefix("//depot/d1/d2").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("id4").pathPrefix("//depot/d1/d3").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("id5").pathPrefix("//depot/d1/d3/d4").build(),
            IntegrationConfig.RepoConfigEntry.builder().repoId("Stg").pathPrefix("//depot/components/STG").build()
    );

    private static final Map<String, String> EXPECTED = new HashMap<>();
    static {
        EXPECTED.put("//depot/d1", "id1");
        EXPECTED.put("//depot/d1/file.txt", "id1");
        EXPECTED.put("//depot/d2", "id2");
        EXPECTED.put("//depot/d2/file.txt", "id2");
        EXPECTED.put("//depot/d1/d2", "id3");
        EXPECTED.put("//depot/d1/d2/file.txt", "id3");
        EXPECTED.put("//depot/d1/d3", "id4");
        EXPECTED.put("//depot/d1/d3/file.txt", "id4");
        EXPECTED.put("//depot/d1/d3/d4", "id5");
        EXPECTED.put("//depot/d1/d3/d4/file.txt", "id5");

        EXPECTED.put(null, null);
        EXPECTED.put("", null);
        EXPECTED.put("//depot", null);
        EXPECTED.put("//depot/doesnotexist/d3/d4/file.txt", null);
        EXPECTED.put("//depot/components/STG/STG_Installer/src/PBA-Jess-TEST/CM/conan/res/defutils.conf", "Stg");
    }
    private static final Map<String, String> CASE_INSENSETIVE_EXPECTED = new HashMap<>(EXPECTED);
    private static final Map<String, String> CASE_SENSETIVE_EXPECTED = new HashMap<>(EXPECTED);
    static {
        CASE_INSENSETIVE_EXPECTED.put("//depot/Components/STG/STG_Installer/src/PBA-Jess-TEST/CM/bldvars.btm", "Stg");
        CASE_SENSETIVE_EXPECTED.put("//depot/Components/STG/STG_Installer/src/PBA-Jess-TEST/CM/bldvars.btm", null);
    }

    @Test
    public void emptyConfig() {
        RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(List.of());
        for(Map.Entry<String, String> x : EXPECTED.entrySet()) {
            Assert.assertEquals(null, repoConfigEntryMatcher.matchPrefix(x.getKey()));
        }
    }
    @Test
    public void nullConfig() {
        RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(null);
        for(Map.Entry<String, String> x : EXPECTED.entrySet()) {
            Assert.assertEquals(null, repoConfigEntryMatcher.matchPrefix(x.getKey()));
        }
    }

    @Test
    public void validConfigCaseInSensitive() {
        RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(CONFIG_ENTRIES);
        for(Map.Entry<String, String> x : CASE_INSENSETIVE_EXPECTED.entrySet()) {
            Assert.assertEquals("Incorrect for " + x.getKey(), x.getValue(), repoConfigEntryMatcher.matchPrefix(x.getKey()));
        }
    }

    @Test
    public void validConfigCaseSensitive() {
        RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(CONFIG_ENTRIES, CASE_SENSITIVE);
        for(Map.Entry<String, String> x : CASE_SENSETIVE_EXPECTED.entrySet()) {
            Assert.assertEquals("Incorrect for " + x.getKey(), x.getValue(), repoConfigEntryMatcher.matchPrefix(x.getKey()));
        }
    }
}