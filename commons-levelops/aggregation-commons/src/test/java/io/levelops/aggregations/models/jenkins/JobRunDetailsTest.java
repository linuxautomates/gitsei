package io.levelops.aggregations.models.jenkins;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobRunDetailsTest {
    @Test
    public void testComparable(){
        List<JobRunDetails> list = new ArrayList<>();
        for(int i=10; i>0; i--){
            JobRunDetails jobRunDetails = JobRunDetails.builder()
                    .number(Long.valueOf(i))
                    .status("SUCCESS")
                    .startTime((long) (i * i))
                    .duration(10L)
                    .userId("userId")
                    .build();
            list.add(jobRunDetails);
        }

        Collections.sort(list);
        int i=1;
        for(JobRunDetails current : list){
            Assert.assertEquals(current.getNumber().intValue(),  i);
            Assert.assertEquals(current.getStartTime().intValue(),  i * i);
            i++;
        }
    }

}