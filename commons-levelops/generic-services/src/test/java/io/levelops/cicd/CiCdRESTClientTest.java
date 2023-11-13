package io.levelops.cicd;

import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CiCdRESTClientTest {

    @Test
    void testDeserialize() throws IOException {
        String json = ResourceUtils.getResourceAsString("json/job_run_stages.json");
        DbListResponse<JobRunStage> o = DefaultObjectMapper.get().readValue(json, DbListResponse.typeOf(DefaultObjectMapper.get(), JobRunStage.class));
        DefaultObjectMapper.prettyPrint(o);
        assertThat(o.getTotalCount()).isEqualTo(5);
        assertThat(o.getRecords()).hasSize(5);
    }

}