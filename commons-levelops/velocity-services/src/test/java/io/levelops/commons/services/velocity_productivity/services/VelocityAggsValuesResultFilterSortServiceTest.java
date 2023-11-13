package io.levelops.commons.services.velocity_productivity.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VelocityAggsValuesResultFilterSortServiceTest {
    private static final ObjectMapper objectMapper = DefaultObjectMapper.get();
    private static final VelocityAggsValuesResultFilterSortService velocityAggsValuesResultFilterSortService = new VelocityAggsValuesResultFilterSortService();

    @Test
    public void test() {
        List<Long> data = new ArrayList<>();
        data.add(15l);
        data.add(12l);
        data.add(null);
        data.add(10l);
        data.add(99l);
        data.add(33l);
        data.add(null);
        data.add(1l);
        //Collections.sort(data, Comparator.nullsLast(Long::compareTo).reversed());
        Collections.sort(data, Comparator.nullsFirst(Long::compareTo).reversed());
        System.out.println(data);
    }

    @Test
    public void testFilterAndSortVelocityValuesTotalNonMissing() throws IOException {
        List<DbAggregationResult> results = objectMapper.readValue(ResourceUtils.getResourceAsString("db_results/velocity_values_results_non_missing.json"), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
        Assert.assertNotNull(results);

        //region Total, 3 ratings
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("good", "needs_attention", "slow"))).build();

        //Total, 3 ratings, default sort - total desc
        DbListResponse<DbAggregationResult> actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(493, actual.getTotalCount().intValue());
        List<String> expectedAdditionalKeys = List.of("LEV-2388","LEV-4442","LEV-4912","LFE-1929","ITOPS-304","LEV-4492","LFE-1982","LEV-4599","LEV-4899","LEV-5041");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, 3 ratings,  sort - total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(493, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-726", "PROP-645", "PROP-290", "LEV-5402", "PROP-402", "ITOPS-526", "PROP-294", "LEV-3807", "ITOPS-534", "PROP-621");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, 3 ratings,  sort - PR Creation Time desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(493, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-1929","LFE-2491","LFE-2014","LEV-4899","LEV-4911","LEV-4950","PROP-2","LFE-2015","PROP-284","LFE-3067");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, 3 ratings,  sort - Time to First Comment asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(493, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-478","ITOPS-469","PROP-805","PROP-411","LEV-5403","PROP-396","PROP-786","PROP-360","PROP-592","PROP-587");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, 3 ratings,  sort - Total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .page(4).pageSize(100)
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(493, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-77","PROP-463","PROP-460","PROP-264","PROP-2","PROP-435","LFE-3067","PROP-391","PROP-269","PROP-359","PROP-338","PROP-299","PROP-296","PROP-427","PROP-298","PROP-425","PROP-247","PROP-550","PROP-551","PROP-305","PROP-300","PROP-314","PROP-574","PROP-392","PROP-242","PROP-380","PROP-266","PROP-546","PROP-286","PROP-285","PROP-196","PROP-188","PROP-258","PROP-213","PROP-522","LFE-3191","LFE-2015","LEV-5116","LEV-5031","PROP-490","PROP-67","PROP-15","LEV-5202","PROP-89","PROP-259","LEV-5017","LFE-2491","PROP-10","PROP-9","LEV-4918","LFE-2014","PROP-212","LEV-5043","LEV-5038","LEV-5036","LEV-5059","LEV-5045","LEV-5044","LEV-5050","LEV-3130","LFE-3095","LEV-4950","LEV-5073","PROP-4","PROP-211","LFE-3084","LFE-3199","LEV-5150","ITOPS-391","LEV-5042","LFE-3078","LFE-3041","LFE-3040","LFE-3037","PROP-12","LEV-5035","ITOPS-372","LEV-5145","LEV-4911","LEV-4914","LFE-3049","LFE-3050","PROP-102","LEV-5041","LEV-4899","LEV-4599","LFE-1982","LEV-4492","ITOPS-304","LFE-1929","LEV-4912","LEV-4442","LEV-2388");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion

        //region Total, good ratings
        defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("good"))).build();

        //Total, good ratings, default sort - total desc
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(470, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LEV-4442","LEV-4912","LFE-1929","LEV-4492","LEV-4599","LEV-4899","LEV-5041","PROP-102","LFE-3050","LFE-3049");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, good ratings,  sort - total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(470, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-726","PROP-645","PROP-290","LEV-5402","PROP-402","ITOPS-526","PROP-294","LEV-3807","ITOPS-534","PROP-621");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, good ratings,  sort - PR Creation Time desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(470, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-1929","LFE-2491","LFE-2014","LEV-4899","LEV-4911","LEV-4950","PROP-2","LFE-2015","PROP-284","LFE-3067");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, good ratings,  sort - Time to First Comment asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(470, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-478","ITOPS-469","PROP-805","PROP-411","LEV-5403","PROP-396","PROP-786","PROP-360","PROP-592","PROP-587");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion

        //region Total, needs_attention ratings
        defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("needs_attention"))).build();

        //Total, needs_attention ratings, default sort - total desc
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(132, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LEV-4442","LEV-4492","LEV-4599","PROP-102","LEV-4914","LEV-4911","LEV-5035","LFE-3199","PROP-211","PROP-4");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, needs_attention ratings,  sort - total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(132, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-2483","PROP-701","PROP-99","PROP-98","PROP-97","LFE-2872","LEV-5387","LFE-3198","LFE-3150","LEV-3666");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, needs_attention ratings,  sort - PR Creation Time desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(132, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LEV-4911","LEV-4950","LFE-3067","LFE-2364","PROP-237","PROP-556","PROP-414","PROP-536","LEV-4599","PROP-325");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, needs_attention ratings,  sort - Time to First Comment asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(132, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LEV-5403","PROP-170","PROP-156","LEV-5043","PROP-536","PROP-701","PROP-187","LEV-5380","LEV-5390","PROP-524");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion

        //region Total, slow ratings
        defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("slow"))).build();

        //Total, slow ratings, default sort - total desc
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(109, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LEV-2388","LEV-4442","LEV-4912","LFE-1929","ITOPS-304","LEV-4492","LFE-1982","LEV-4599","LEV-4899","LEV-5041");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, slow ratings,  sort - total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(109, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-3030","LEV-3551","LEV-4920","PROP-42","LEV-4910","PROP-5","PROP-368","PROP-200","PROP-3","LFE-2684");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, slow ratings,  sort - PR Creation Time desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(109, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-1929","LFE-2491","LFE-2014","LEV-4899","LEV-4911","LEV-4950","PROP-2","LFE-2015","PROP-284","LFE-3067");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, slow ratings,  sort - Time to First Comment asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(109, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LEV-5145","LEV-5017","LEV-5043","LFE-2491","LEV-4912","LEV-4918","PROP-284","PROP-77","LEV-5150","ITOPS-372");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion

        //region Total, 2 ratings
        defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("good", "slow"))).build();

        //Total, 2 ratings, default sort - total desc
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(479, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LEV-2388","LEV-4442","LEV-4912","LFE-1929","ITOPS-304","LEV-4492","LFE-1982","LEV-4599","LEV-4899","LEV-5041");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, 2 ratings,  sort - total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(479, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-726","PROP-645","PROP-290","LEV-5402","PROP-402","ITOPS-526","PROP-294","LEV-3807","ITOPS-534","PROP-621");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, 2 ratings,  sort - PR Creation Time desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(479, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-1929","LFE-2491","LFE-2014","LEV-4899","LEV-4911","LEV-4950","PROP-2","LFE-2015","PROP-284","LFE-3067");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, 2 ratings,  sort - Time to First Comment asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(479, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-478","ITOPS-469","PROP-805","PROP-411","LEV-5403","PROP-396","PROP-786","PROP-360","PROP-592","PROP-587");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion
    }

    @Test
    public void testFilterAndSortVelocityValuesTotalMissing() throws IOException {
        List<DbAggregationResult> results = objectMapper.readValue(ResourceUtils.getResourceAsString("db_results/velocity_values_results_missing.json"), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
        Assert.assertNotNull(results);

        //region Total, missing rating
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("missing")))
                .build();

        //Total, missing rating, default sort - total desc
        DbListResponse<DbAggregationResult> actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(5137, actual.getTotalCount().intValue());
        List<String> expectedAdditionalKeys = List.of("LEV-2388","LFE-1929","ITOPS-304","LFE-1982","LEV-4950","LEV-3130","LEV-5050","LFE-2014","LFE-2491","LFE-2015");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, missing rating,  sort - total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(5137, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-2430","LFE-3015","LEV-3517","ITOPS-249","LFE-1776","LFE-1994","LEV-3392","LFE-894","LEV-4639","LFE-2018");
        //Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, missing rating,  sort - PR Creation Time desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(5137, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-1929","LFE-2491","LFE-2014","LEV-4950","PROP-2","LFE-2015","PROP-284","LFE-3067","PROP-5","PROP-368");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, missing rating,  sort - Time to First Comment asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(5137, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-805","PROP-786","PROP-587","PROP-724","PROP-690","LEV-5237","PROP-789","PROP-621","PROP-402","LFE-3179");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion
    }

    @Test
    public void testFilterAndSortVelocityValuesSingleStageNonMissing() throws IOException {
        List<DbAggregationResult> results = objectMapper.readValue(ResourceUtils.getResourceAsString("db_results/velocity_values_results_non_missing.json"), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
        Assert.assertNotNull(results);

        //region Single Stage, 3 ratings
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("good", "needs_attention", "slow"), "histogram_stage_name", "PR Creation Time"))
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();

        //Single Stage, 3 ratings, sort - Single Stage desc
        DbListResponse<DbAggregationResult> actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(445, actual.getTotalCount().intValue());
        List<String> expectedAdditionalKeys = List.of("LFE-1929","LFE-2491","LFE-2014","LEV-4899","LEV-4911","LEV-4950","PROP-2","LFE-2015","PROP-284","LFE-3067");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Single Stage, 3 ratings,  sort - Single Stage asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(445, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-600","LFE-3150","PROP-12","PROP-267","LFE-3095","LEV-5150","PROP-537","LFE-3041","PROP-292","PROP-296");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Single Stage, 3 ratings,  sort - Total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(445, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-726","PROP-645","PROP-290","LEV-5402","PROP-402","ITOPS-526","PROP-294","ITOPS-534","PROP-621","PROP-647");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Single Stage, 3 ratings,  sort - Different Single Stage desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(445, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-3078","PROP-490","PROP-522","PROP-546","PROP-574","LFE-3050","PROP-380","LFE-3037","LFE-3084","LFE-3049");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Single Stage, 3 ratings,  sort - Different Single Stage desc - page 2, page_size 20
        defaultListRequest = defaultListRequest.toBuilder()
                .page(2).pageSize(20)
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(445, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-2483","PROP-119","PROP-204","PROP-611","PROP-736","LFE-3179","PROP-563","PROP-690","LFE-2794","PROP-567","PROP-610","PROP-361","PROP-702","LFE-3191","PROP-798","PROP-790","PROP-650","PROP-405","PROP-504","PROP-232");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //endregion

        //region good Stage, 1 rating
        defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("good"), "histogram_stage_name", "PR Creation Time"))
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();

        //good Stage, 1 rating, sort - Single Stage desc
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(401, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-736","LFE-3179","PROP-563","PROP-690","LFE-2794","PROP-567","PROP-610","PROP-361","PROP-702","LFE-3191");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //good Stage, 1 rating,  sort - Single Stage asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(401, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-600","LFE-3150","PROP-12","PROP-267","LFE-3095","LEV-5150","PROP-537","LFE-3041","PROP-292","PROP-296");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //good Stage, 1 rating,  sort - Total asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(401, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("PROP-726","PROP-645","PROP-290","LEV-5402","PROP-402","ITOPS-526","PROP-294","ITOPS-534","PROP-621","PROP-647");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //good Stage, 1 rating,  sort - Different Single Stage desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(401, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-3078","PROP-490","PROP-522","PROP-546","PROP-574","LFE-3050","PROP-380","LFE-3037","LFE-3084","LFE-3049");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion

        //region needs_attention Stage, 1 rating
        defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("needs_attention"), "histogram_stage_name", "PR Creation Time"))
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();

        //needs_attention Stage, 1 rating, sort - Single Stage desc
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(30, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-2364","PROP-237","PROP-556","PROP-414","PROP-536","LEV-4599","PROP-325","LFE-2596","PROP-146","PROP-370");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion

        //region slow Stage, 1 rating
        defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("slow"), "histogram_stage_name", "PR Creation Time"))
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();

        //slow Stage, 1 rating, sort - Single Stage desc
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(14, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-1929","LFE-2491","LFE-2014","LEV-4899","LEV-4911","LEV-4950","PROP-2","LFE-2015","PROP-284","LFE-3067");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion

        //region needs_attention,slow Stage, 1 rating
        defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("needs_attention", "slow"), "histogram_stage_name", "PR Creation Time"))
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();

        //slow Stage, 1 rating, sort - Single Stage desc
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(44, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-1929","LFE-2491","LFE-2014","LEV-4899","LEV-4911","LEV-4950","PROP-2","LFE-2015","PROP-284","LFE-3067");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion
    }

    @Test
    public void testFilterAndSortVelocityValuesSingleStageMissing() throws IOException {
        List<DbAggregationResult> results = objectMapper.readValue(ResourceUtils.getResourceAsString("db_results/velocity_values_results_missing.json"), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
        Assert.assertNotNull(results);

        //region Single Stage, missing rating
        DefaultListRequest defaultListRequest = DefaultListRequest.builder()
                .page(0).pageSize(10)
                .filter(Map.of("ratings", List.of("missing"), "histogram_stage_name", "PR Creation Time"))
                .sort(List.of(Map.of("id","PR Creation Time", "desc",false)))
                .build();

        //Total, missing rating, sort - single stage asc
        DbListResponse<DbAggregationResult> actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(4900, actual.getTotalCount().intValue());
        List<String> expectedAdditionalKeys = List.of("LFE-1987","LEV-5306","LFE-2420","LFE-3065","LFE-1248","PROP-168","LFE-2636","LFE-3063","LEV-4315","QUAL-294");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, missing rating, sort - single stage desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","PR Creation Time", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(4900, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-874", "LEV-3964", "ITOPS-258", "ITOPS-463", "LFE-1976", "LEV-3196", "LEV-3848", "LFE-2055", "LFE-2865", "LEV-4524");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, missing rating, sort - total desc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","total", "desc",true))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(4900, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LEV-2388","ITOPS-304","LFE-1982","LEV-3130","LEV-5050","LFE-2684","LEV-4910","LEV-4920","LEV-3551","LEV-5196");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));

        //Total, missing rating,  sort - Time to First Comment asc
        defaultListRequest = defaultListRequest.toBuilder()
                .sort(List.of(Map.of("id","Time to First Comment", "desc",false))).build();
        actual = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, defaultListRequest);
        Assert.assertNotNull(actual);
        Assert.assertEquals(4900, actual.getTotalCount().intValue());
        expectedAdditionalKeys = List.of("LFE-1987","LEV-5306","LFE-2420","LFE-3065","LFE-1248","PROP-168","LFE-2636","LFE-3063","LEV-4315","QUAL-294");
        Assert.assertEquals(expectedAdditionalKeys, actual.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()));
        //endregion
    }
}