package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobConfigChangeDetailTest {
    @Test
    public void testSerializeDeSerialize() throws JsonProcessingException {
        JobConfigChangeDetail jobConfigChangeDetail = JobConfigChangeDetail.builder()
                .changeTime(Instant.now().getEpochSecond()).operation("CREATED").userId("viraj").userFullName("Viraj Ajgaonkar")
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writeValueAsString(jobConfigChangeDetail);
        JobConfigChangeDetail actual = objectMapper.readValue(serialized, JobConfigChangeDetail.class);
        Assert.assertEquals(jobConfigChangeDetail, actual);
    }

    @Test
    public void testComparable(){
        List<JobConfigChangeDetail> list = new ArrayList<>();
        for(int i=10; i>0; i--){
            JobConfigChangeDetail t = JobConfigChangeDetail.builder()
                    .changeTime(Long.valueOf(i))
                    .operation("Created")
                    .userId("testVa")
                    .build();
            list.add(t);
        }
        Collections.sort(list);
        int i=1;
        for(JobConfigChangeDetail current : list){
            Assert.assertEquals(current.getChangeTime(), Long.valueOf(i));
            i++;
        }
    }
}