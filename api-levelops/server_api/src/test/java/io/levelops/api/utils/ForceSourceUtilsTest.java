package io.levelops.api.utils;

import org.junit.Assert;
import org.junit.Test;

public class ForceSourceUtilsTest {
    @Test
    public void testUseES() {
        Assert.assertTrue(ForceSourceUtils.useES("es"));
        Assert.assertFalse(ForceSourceUtils.useES("db"));
        Assert.assertNull(ForceSourceUtils.useES(null));
        Assert.assertNull(ForceSourceUtils.useES(""));
        Assert.assertNull(ForceSourceUtils.useES(" "));
        Assert.assertNull(ForceSourceUtils.useES("DB"));
        Assert.assertNull(ForceSourceUtils.useES("ES"));
        Assert.assertNull(ForceSourceUtils.useES("Db"));
        Assert.assertNull(ForceSourceUtils.useES("Es"));
    }
}