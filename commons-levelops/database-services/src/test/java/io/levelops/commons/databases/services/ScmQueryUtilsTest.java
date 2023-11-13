package io.levelops.commons.databases.services;

import io.levelops.commons.databases.utils.CommonUtils;
import io.levelops.ingestion.models.IntegrationType;
import org.junit.Assert;
import org.junit.Test;
import java.util.Map;

public class ScmQueryUtilsTest {

    @Test
    public void testIsScmIntegration() {
        for(IntegrationType it : IntegrationType.values()) {
            Assert.assertEquals(it.isScmFamily(), ScmQueryUtils.isScmIntegration(it));
        }

        Assert.assertFalse(ScmQueryUtils.isScmIntegration(null));
    }

    @Test
    public void getCodeChangeSql_WhenFilesCodeChangeUnit()
    {
        String expectedQuery=", CASE  WHEN files_ct IS NULL OR files_ct <= 50 THEN 'small' WHEN files_ct > 50 AND  files_ct <= 150 THEN 'medium' WHEN files_ct > 150 THEN 'large' END code_change";
        String actualQuery= ScmQueryUtils.getCodeChangeSql(Map.of(),true,"files");
        Assert.assertEquals(expectedQuery,actualQuery);
    }

    @Test
    public void getCodeChangeSql_WhenLinesCodeChangeUnit()
    {
        String expectedQuery=", CASE  WHEN lines_changed IS NULL OR lines_changed <= 50 THEN 'small' WHEN lines_changed > 50 AND  lines_changed <= 150 THEN 'medium' WHEN lines_changed > 150 THEN 'large' END code_change";
        String actualQuery= ScmQueryUtils.getCodeChangeSql(Map.of(),true,"lines_changed");
        Assert.assertEquals(expectedQuery,actualQuery);
    }
    @Test
    public void getCodeChangeSql_WhenNullCodeChangeUnit()
    {
        String expectedQuery=", CASE  WHEN lines_changed IS NULL OR lines_changed <= 50 THEN 'small' WHEN lines_changed > 50 AND  lines_changed <= 150 THEN 'medium' WHEN lines_changed > 150 THEN 'large' END code_change";
        String actualQuery= ScmQueryUtils.getCodeChangeSql(Map.of(),true,null);
        Assert.assertEquals(expectedQuery,actualQuery);
    }

    @Test
    public void testGetUseMergeShaForCommitsJoinFlag() {
        Boolean useMergeShaForCommitsJoinFlag = CommonUtils.getUseMergeShaForCommitsJoinFlag("test");
        Assert.assertFalse(useMergeShaForCommitsJoinFlag);
    }
}