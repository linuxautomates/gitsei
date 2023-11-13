package io.levelops.commons.databases.models.database.scm.converters.gerrit;

import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GerritFileConvertersTest {

    @Test
    public void parseCommitFiles() throws IOException {
        ChangeInfo changeInfo = ResourceUtils.getResourceAsObject("gerrit/gerrit_change2.json", ChangeInfo.class);
        List<DbScmFile> files = GerritFileConverters.parseCommitFiles("1", changeInfo, changeInfo.getCurrentRevision());
        DefaultObjectMapper.prettyPrint(files);

        List<DbScmFile> expected = ResourceUtils.getResourceAsObject("gerrit/expected_files2.json",
                DefaultObjectMapper.get().getTypeFactory().constructCollectionType(List.class, DbScmFile.class));
        assertThat(files).containsExactlyInAnyOrderElementsOf(expected);
    }

}