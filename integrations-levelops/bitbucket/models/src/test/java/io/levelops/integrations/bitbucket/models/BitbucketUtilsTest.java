package io.levelops.integrations.bitbucket.models;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static io.levelops.integrations.bitbucket.models.BitbucketUtils.BITBUCKET_DATE_FORMATTER;

public class BitbucketUtilsTest {
    @Test
    public void testSerialize(){
        Date now = new Date();
        String dateString = BITBUCKET_DATE_FORMATTER.format(now);
        System.out.println(dateString);
    }

    @Test
    public void testDeSerialize() throws ParseException {
        String dateString = "2020-02-11T23:03:42.935599+00:00";
        Date date = BITBUCKET_DATE_FORMATTER.parse(dateString);
        System.out.println(date);
        Assert.assertNotNull(date);
    }

    @Test
    public void testDeSerialize2() throws ParseException {
        String dateString = "2020-02-12T00:37:38.433950+00:00";
        Date date = BITBUCKET_DATE_FORMATTER.parse(dateString);
        System.out.println(date);
        Assert.assertNotNull(date);
    }

    @Test
    public void testDeSerialize3() throws ParseException {
        String dateString = "2020-02-12T00:37:38.000000+00:00";
        Date date = BITBUCKET_DATE_FORMATTER.parse(dateString);
        System.out.println(date);
        Assert.assertNotNull(date);
    }



}