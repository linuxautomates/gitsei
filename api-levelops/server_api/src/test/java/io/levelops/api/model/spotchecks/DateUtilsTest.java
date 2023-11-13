package io.levelops.api.model.spotchecks;


import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class DateUtilsTest {
    @Test
    public void testRequestStrToDate() {
        Date d = DateUtils.requestStrToDate("05/01/2023");
        Assert.assertNotNull(d);
    }

}