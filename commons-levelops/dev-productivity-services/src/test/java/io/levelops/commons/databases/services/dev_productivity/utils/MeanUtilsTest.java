package io.levelops.commons.databases.services.dev_productivity.utils;

import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import org.junit.Test;

import static io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile.FeatureType.AVG_CODING_DAYS_PER_WEEK;
import static org.assertj.core.api.Assertions.assertThat;

public class MeanUtilsTest {

    @Test
    public void testMean() {
        assertThat(MeanUtils.convertMeanDaysToSecondsOrDefault(null, 0.0000019d)).isEqualTo(0.16416d);
        assertThat(MeanUtils.roundMeanDoubleUpToLong(0.16416d)).isEqualTo(1);

        assertThat(MeanUtils.convertMeanDaysToSecondsOrDefault(null, null)).isEqualTo(null);
        assertThat(MeanUtils.convertMeanDaysToSecondsOrDefault(DevProductivityProfile.Feature.builder().featureType(AVG_CODING_DAYS_PER_WEEK).build(), null)).isEqualTo(0L);
        assertThat(MeanUtils.roundMeanDoubleUpToLong(null)).isEqualTo(null);

        assertThat(MeanUtils.roundMeanDoubleUpToLong(0d)).isEqualTo(0);
        assertThat(MeanUtils.roundMeanDoubleUpToLong(0.0000000000001d)).isEqualTo(1);
    }

}