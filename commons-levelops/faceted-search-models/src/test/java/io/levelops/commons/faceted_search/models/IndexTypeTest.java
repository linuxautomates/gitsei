package io.levelops.commons.faceted_search.models;

import org.junit.Assert;
import org.junit.Test;

public class IndexTypeTest {
    private static final String COMPANY = "test";
    @Test
    public void testGetCombinedIndexAlias() {
        //For any changes to RequestType, modify this test
        Assert.assertTrue(IndexType.values().length == 3);
        Assert.assertEquals("work_items_test_combined", IndexType.WORK_ITEMS.getCombinedIndexAlias(COMPANY));
        Assert.assertEquals("scm_commits_test_combined", IndexType.SCM_COMMITS.getCombinedIndexAlias(COMPANY));
        Assert.assertEquals("scm_prs_test_combined", IndexType.SCM_PRS.getCombinedIndexAlias(COMPANY));
    }

    @Test
    public void testGetIndexWildcard() {
        //For any changes to RequestType, modify this test
        Assert.assertTrue(IndexType.values().length == 3);
        Assert.assertEquals("work_items_test_*", IndexType.WORK_ITEMS.getIndexWildcard(COMPANY));
        Assert.assertEquals("scm_commits_test", IndexType.SCM_COMMITS.getIndexWildcard(COMPANY));
        Assert.assertEquals("scm_prs_test", IndexType.SCM_PRS.getIndexWildcard(COMPANY));
    }

    @Test
    public void testGetPartitionedIndexName() {
        //For any changes to RequestType, modify this test
        Assert.assertTrue(IndexType.values().length == 3);
        Assert.assertEquals("work_items_test_1612137600", IndexType.WORK_ITEMS.getPartitionedIndexName(COMPANY, 1612137600l));
        Assert.assertEquals("scm_commits_test", IndexType.SCM_COMMITS.getPartitionedIndexName(COMPANY, 1612137600l));
        Assert.assertEquals("scm_prs_test", IndexType.SCM_PRS.getPartitionedIndexName(COMPANY, 1612137600l));
    }
}