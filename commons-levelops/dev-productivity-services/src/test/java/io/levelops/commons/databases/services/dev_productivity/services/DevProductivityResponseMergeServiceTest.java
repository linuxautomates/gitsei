package io.levelops.commons.databases.services.dev_productivity.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.services.dev_productivity.engine.DevProductivityEngine;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DevProductivityResponseMergeServiceTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String email = "sanjay.gurnani@broadcom.com";
    private static final String fullName = "Sanjay Gurnani";
    private static final UUID orgUserId = UUID.randomUUID();
    @Test
    public void testMergeDevProductivityResponses() throws IOException {
        DevProductivityProfile profile = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/dev_prod_profile_2.json"), DevProductivityProfile.class);

        DevProductivityResponse existing = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/existing_response_1.json"), DevProductivityResponse.class);
        DevProductivityResponse newResponse = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/new_response_1.json"), DevProductivityResponse.class);
        DevProductivityResponse expected = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_1.json"), DevProductivityResponse.class);

        DevProductivityResponseMergeService service = new DevProductivityResponseMergeService();
        DevProductivityResponse actual = service.mergeDevProductivityResponses(profile, existing, newResponse, null);
        Assert.assertEquals(expected, actual);

        Assert.assertFalse(service.devProductivityResponseHasChanged(profile, null, null));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, null, existing));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, existing, null));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, existing, newResponse));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, existing, actual));
        Assert.assertFalse(service.devProductivityResponseHasChanged(profile, existing, existing));
        Assert.assertFalse(service.devProductivityResponseHasChanged(profile, newResponse, newResponse));
        Assert.assertFalse(service.devProductivityResponseHasChanged(profile, actual, actual));
    }
    @Test
    public void testMergeDevProductivityResponses3() throws IOException {
        DevProductivityProfile profile = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/dev_prod_profile_2.json"), DevProductivityProfile.class);

        DevProductivityResponse expected = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_1.json"), DevProductivityResponse.class);
        DevProductivityResponse incorrect1 = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_incorrect_1.json"), DevProductivityResponse.class);
        DevProductivityResponse incorrect2 = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_incorrect_2.json"), DevProductivityResponse.class);
        DevProductivityResponse incorrect3 = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_incorrect_3.json"), DevProductivityResponse.class);
        DevProductivityResponse incorrect4 = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_incorrect_4.json"), DevProductivityResponse.class);
        DevProductivityResponse incorrect5 = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_incorrect_5.json"), DevProductivityResponse.class);
        DevProductivityResponse incorrect6 = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_incorrect_6.json"), DevProductivityResponse.class);
        DevProductivityResponse incorrect7 = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_incorrect_7.json"), DevProductivityResponse.class);

        DevProductivityResponseMergeService service = new DevProductivityResponseMergeService();
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, expected, incorrect1));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, expected, incorrect2));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, expected, incorrect3));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, expected, incorrect4));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, expected, incorrect5));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, expected, incorrect6));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, expected, incorrect7));
    }

    @Test
    public void testMergeDevProductivityResponses2() throws IOException {
        DevProductivityProfile profile = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/dev_prod_profile_2.json"), DevProductivityProfile.class);

        Map<Integer, Map<Integer, FeatureResponse>> existingFRMap = new HashMap<>();
        Map<Integer, Map<Integer, FeatureResponse>> newFRMap = new HashMap<>();
        Map<Integer, Map<Integer, FeatureResponse>> mergedFRMap = new HashMap<>();

        AtomicInteger i = new AtomicInteger();
        Random random = new Random(System.currentTimeMillis());

        for(DevProductivityProfile.Section s : CollectionUtils.emptyIfNull(profile.getSections())) {
            for(DevProductivityProfile.Feature f : CollectionUtils.emptyIfNull(s.getFeatures())) {
                int sectionOrder = s.getOrder();
                int featureOrder = f.getOrder();

                int mod = i.getAndIncrement() % 4;
                FeatureResponse e = FeatureResponse.constructBuilder(sectionOrder, f, (long) random.nextInt(100)).build();
                FeatureResponse n = FeatureResponse.constructBuilder(sectionOrder, f, (long) random.nextInt(100)).build();

                if(mod == 0) {
                    continue;
                } else if (mod == 1) {
                    existingFRMap.computeIfAbsent(sectionOrder,k -> new HashMap<>()).put(featureOrder, e);
                    mergedFRMap.computeIfAbsent(sectionOrder,k -> new HashMap<>()).put(featureOrder, e);
                } else if (mod == 2) {
                    newFRMap.computeIfAbsent(sectionOrder,k -> new HashMap<>()).put(featureOrder, n);
                    mergedFRMap.computeIfAbsent(sectionOrder,k -> new HashMap<>()).put(featureOrder, n);
                } else if (mod == 3) {
                    existingFRMap.computeIfAbsent(sectionOrder,k -> new HashMap<>()).put(featureOrder, e);
                    newFRMap.computeIfAbsent(sectionOrder,k -> new HashMap<>()).put(featureOrder, n);
                    mergedFRMap.computeIfAbsent(sectionOrder,k -> new HashMap<>()).put(featureOrder, n);
                }
            }
        }

        DevProductivityResponse existing = DevProductivityEngine.ResponseHelper.buildResponseFromFullFeatureResponses(profile, existingFRMap).toBuilder()
                .orgUserId(orgUserId).fullName(fullName).email(email)
                .build();
        DevProductivityResponse newResponse = DevProductivityEngine.ResponseHelper.buildResponseFromFullFeatureResponses(profile, newFRMap).toBuilder()
                .orgUserId(orgUserId).fullName(fullName).email(email)
                .build();
        DevProductivityResponse expected = DevProductivityEngine.ResponseHelper.buildResponseFromFullFeatureResponses(profile, mergedFRMap).toBuilder()
                .orgUserId(orgUserId).fullName(fullName).email(email)
                .build();

        DevProductivityResponseMergeService service = new DevProductivityResponseMergeService();
        DevProductivityResponse actual = service.mergeDevProductivityResponses(profile, existing, newResponse, null);
        Assert.assertEquals(expected, actual);

        Assert.assertFalse(service.devProductivityResponseHasChanged(profile, null, null));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, null, existing));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, existing, null));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, existing, newResponse));
        Assert.assertTrue(service.devProductivityResponseHasChanged(profile, existing, actual));
        Assert.assertFalse(service.devProductivityResponseHasChanged(profile, existing, existing));
        Assert.assertFalse(service.devProductivityResponseHasChanged(profile, newResponse, newResponse));
        Assert.assertFalse(service.devProductivityResponseHasChanged(profile, actual, actual));
    }

    @Test
    public void testMergeWhenProfileChanges() throws IOException {
        DevProductivityProfile profile = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/dev_prod_profile_1.json"), DevProductivityProfile.class);

        DevProductivityResponse existing = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_2.json"), DevProductivityResponse.class);
        DevProductivityResponse newResponse = null;
        DevProductivityResponse expected = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/merged_response_3.json"), DevProductivityResponse.class);

        DevProductivityResponseMergeService service = new DevProductivityResponseMergeService();
        DevProductivityResponse actual = service.mergeDevProductivityResponses(profile, existing, newResponse, null);
        System.out.println(MAPPER.writeValueAsString(actual));
        Assert.assertEquals(expected, actual);
    }
}