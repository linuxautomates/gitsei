package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.RepoConfigEntryMatcher;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class HelixCoreAggHelperTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final List<IntegrationConfig.RepoConfigEntry> REPO_CONFIG_ENTRY_LIST = List.of(
            IntegrationConfig.RepoConfigEntry.builder().repoId("Stg").pathPrefix("//depot/components/STG").build()
    );
    private static final RepoConfigEntryMatcher REPO_CONFIG_ENTRY_MATCHER = new RepoConfigEntryMatcher(REPO_CONFIG_ENTRY_LIST);
    private static final String CUSTOMER = "test";
    private static final String INTEGRATION_ID = "1";

    @Mock
    private ScmAggService scmAggService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test() throws IOException, SQLException {
        String content = ResourceUtils.getResourceAsString("helix/helix_core_change_lists.json");
        List<HelixCoreChangeList> changeLists = MAPPER.readValue(content, MAPPER.getTypeFactory().constructCollectionType(List.class, HelixCoreChangeList.class));

        ArgumentCaptor<DbScmCommit> commitArgumentCaptor = ArgumentCaptor.forClass(DbScmCommit.class);

        List<IntegrationWhitelistEntry> scmCommitsInsertV2integrationIdWhitelist = IntegrationWhitelistEntry.fromCommaSeparatedString("abcd:1234");
        HelixCoreAggHelper helixCoreAggHelper = new HelixCoreAggHelper(null, scmAggService, null, scmCommitsInsertV2integrationIdWhitelist);
        boolean useScmCommitInsertV2 = ScmAggUtils.useScmCommitsInsertV2(scmCommitsInsertV2integrationIdWhitelist, CUSTOMER, INTEGRATION_ID);
        for(HelixCoreChangeList changeList : changeLists) {
            helixCoreAggHelper.processHelixChangeList(CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_LIST, changeList, Map.of(), useScmCommitInsertV2);
        }
        verify(scmAggService, times(2)).insertCommit(eq(CUSTOMER), commitArgumentCaptor.capture());
        assertThat(2).isEqualTo(commitArgumentCaptor.getAllValues().size());
        for(DbScmCommit scmCommit : commitArgumentCaptor.getAllValues()) {
            Assert.assertEquals(1, scmCommit.getRepoIds().size());
            Assert.assertEquals("Stg", scmCommit.getRepoIds().get(0));
        }
    }
}