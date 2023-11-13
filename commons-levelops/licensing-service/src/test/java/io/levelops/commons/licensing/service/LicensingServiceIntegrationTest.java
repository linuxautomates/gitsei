package io.levelops.commons.licensing.service;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.licensing.exception.LicensingException;
import io.levelops.commons.licensing.model.License;
import junit.framework.TestCase;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LicensingServiceIntegrationTest {
    @Test
    public void test() throws LicensingException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(1, TimeUnit.MINUTES) // connect timeout
                .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                .readTimeout(5, TimeUnit.MINUTES); // read timeout
        OkHttpClient client = builder.build();

        LicensingService service = LicensingService.builder()
                .licensingServiceUrl("http://localhost:10080/").client(client).objectMapper(DefaultObjectMapper.get())
                .build();

            List<String> entitlements1 = List.of("ALL_FEATURES", "DASHBOARDS", "ISSUES", "PROPELS", "TRIAGE", "TEMPLATES", "REPORTS", "TABLES", "SETTINGS", "SELECTABLE_DRILLDOWN_COLUMNS", "NEW_DORA_LEADTIME_WIDGET", "DORA_IMPROVEMENTS","DORA_IMPROVEMENTS_2");
        List<String> entitlements2 = List.of("ALL_FEATURES", "DASHBOARDS", "ISSUES", "PROPELS", "TRIAGE", "TEMPLATES", "REPORTS", "TABLES", "SETTINGS", "SELECTABLE_DRILLDOWN_COLUMNS", "NEW_DORA_LEADTIME_WIDGET", "DORA_IMPROVEMENTS");

        License updated = service.updateEntitlements("foo", entitlements1);
        Assert.assertTrue(updated.getEntitlements().contains("DORA_IMPROVEMENTS_2"));

        updated = service.updateEntitlements("foo", entitlements2);
        Assert.assertFalse(updated.getEntitlements().contains("DORA_IMPROVEMENTS_2"));

        List<String> before = updated.getEntitlements();

        updated = service.appendEntitlements("foo", List.of("DORA_IMPROVEMENTS_3", "DORA_IMPROVEMENTS_4"));
        List<String> newList = updated.getEntitlements();

        Assert.assertTrue(newList.contains("DORA_IMPROVEMENTS_3"));
        Assert.assertTrue(newList.contains("DORA_IMPROVEMENTS_4"));
        for(String e : before) {
            Assert.assertTrue(newList.contains(e));
        }
        Assert.assertEquals(2, newList.size() - before.size());

        service.updateEntitlements("foo", before);
    }
}