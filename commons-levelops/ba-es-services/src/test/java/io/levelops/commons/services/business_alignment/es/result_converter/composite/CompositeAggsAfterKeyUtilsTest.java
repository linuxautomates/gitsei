package io.levelops.commons.services.business_alignment.es.result_converter.composite;

import org.junit.Assert;
import org.junit.Test;

public class CompositeAggsAfterKeyUtilsTest {
    @Test
    public void test() {
        Assert.assertEquals(null, CompositeAggsAfterKeyUtils.formatStringData(null));
        Assert.assertEquals("", CompositeAggsAfterKeyUtils.formatStringData(""));
        Assert.assertEquals(" ", CompositeAggsAfterKeyUtils.formatStringData(" "));
        Assert.assertEquals("abc", CompositeAggsAfterKeyUtils.formatStringData("abc"));
        Assert.assertEquals("abc", CompositeAggsAfterKeyUtils.formatStringData("\"abc"));
        Assert.assertEquals("abc", CompositeAggsAfterKeyUtils.formatStringData("abc\""));
        Assert.assertEquals("abc", CompositeAggsAfterKeyUtils.formatStringData("\"abc\""));
        Assert.assertEquals("samuel.djiani", CompositeAggsAfterKeyUtils.formatStringData("\"samuel.djiani\""));
        Assert.assertEquals("0090ffec-b8dd-4285-830f-c7d45f7d9c96", CompositeAggsAfterKeyUtils.formatStringData("\"0090ffec-b8dd-4285-830f-c7d45f7d9c96\""));
    }
}