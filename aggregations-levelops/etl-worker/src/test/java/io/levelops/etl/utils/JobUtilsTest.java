package io.levelops.etl.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.job_progress.EntityProgressDetail;
import io.levelops.commons.etl.models.job_progress.FileProgressDetail;
import io.levelops.commons.etl.models.job_progress.StageProgressDetail;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.etl.job_framework.EtlProcessor;
import io.levelops.utils.SampleJobStage;
import io.levelops.utils.TestEtlProcessor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class JobUtilsTest {
    @Mock
    EtlProcessor<TestEtlProcessor.TestJobState> ETLProcessor;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(ETLProcessor.getJobStages()).thenReturn(List.of(new SampleJobStage()));
    }

    @Test
    public void testDetermineJobStatus() {
        Map<String, StageProgressDetail> partialSuccessProgressDetailMap = Map.of("test", StageProgressDetail.builder()
                .fileProgressMap(Map.of(
                        0, FileProgressDetail.builder()
                                .entityProgressDetail(EntityProgressDetail.builder()
                                        .totalEntities(10)
                                        .successful(8)
                                        .failed(2)
                                        .build())
                                .failures(List.of())
                                .durationMilliSeconds(100L)
                                .build(),
                        1, FileProgressDetail.builder()
                                .entityProgressDetail(EntityProgressDetail.builder()
                                        .totalEntities(20)
                                        .successful(18)
                                        .failed(2)
                                        .build())
                                .failures(List.of())
                                .durationMilliSeconds(100L)
                                .build()))
                .build());

        Map<String, StageProgressDetail> successProgressDetailMap = Map.of("test", StageProgressDetail.builder()
                .fileProgressMap(Map.of(
                        0, FileProgressDetail.builder()
                                .entityProgressDetail(EntityProgressDetail.builder()
                                        .totalEntities(10)
                                        .successful(10)
                                        .failed(0)
                                        .build())
                                .failures(List.of())
                                .durationMilliSeconds(100L)
                                .build(),
                        1, FileProgressDetail.builder()
                                .entityProgressDetail(EntityProgressDetail.builder()
                                        .totalEntities(20)
                                        .successful(20)
                                        .failed(0)
                                        .build())
                                .failures(List.of())
                                .durationMilliSeconds(100L)
                                .build()))
                .build());

        Map<String, StageProgressDetail> successProgressDetailMapWithEmptyStage = Map.of("test", StageProgressDetail.builder()
                .fileProgressMap(Map.of())
                .build());

        assertThat(JobUtils.determineJobSuccessStatus(partialSuccessProgressDetailMap, ETLProcessor)).isEqualTo(JobStatus.PARTIAL_SUCCESS);
        assertThat(JobUtils.determineJobSuccessStatus(successProgressDetailMap, ETLProcessor)).isEqualTo(JobStatus.SUCCESS);
        assertThat(JobUtils.determineJobSuccessStatus(successProgressDetailMapWithEmptyStage, ETLProcessor)).isEqualTo(JobStatus.SUCCESS);
        assertThat(JobUtils.determineJobSuccessStatus(Collections.emptyMap(), ETLProcessor)).isEqualTo(JobStatus.FAILURE);
    }

    @Test
    public void testJiraDetermineJobStatus() throws IOException {
        when(ETLProcessor.getJobStages()).thenReturn(List.of(new SampleJobStage(), new SampleJobStage(), new SampleJobStage(), new SampleJobStage(), new SampleJobStage(), new SampleJobStage()));

        String str = ResourceUtils.getResourceAsString("job_progress_detail.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        var progressDetailMap = ParsingUtils.parseMap(mapper, "progress_details", String.class, StageProgressDetail.class, str);
        assertThat(JobUtils.determineJobSuccessStatus(progressDetailMap, ETLProcessor)).isEqualTo(JobStatus.SUCCESS);
    }
}