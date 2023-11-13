package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DbTestRailsTestCaseTest {

    public ObjectMapper mapper = DefaultObjectMapper.get();

    @Test
    public void testDbTestRailsTestCase() throws IOException {
        List<TestRailsTestCase> testCases = ResourceUtils.getResourceAsObject("testrails/testrails-test-case.json",
                mapper.getTypeFactory().constructCollectionType(List.class, TestRailsTestCase.class));
        List<DbTestRailsTestCase> dbTestRailsTestCases = testCases.stream()
                .map(testCase -> DbTestRailsTestCase.fromTestCase(testCase, "1"))
                .collect(Collectors.toList());
        Assert.assertNotNull(dbTestRailsTestCases);
        Assert.assertEquals(3, dbTestRailsTestCases.size());
        Assert.assertEquals(3660L, (long)dbTestRailsTestCases.get(0).getEstimate());
        Assert.assertEquals(3660, (long)dbTestRailsTestCases.get(0).getEstimateForecast());
        Assert.assertEquals(1212, (long)dbTestRailsTestCases.get(1).getEstimate());
        Assert.assertEquals(1212, (long)dbTestRailsTestCases.get(1).getEstimateForecast());
        Assert.assertEquals(0, (long)dbTestRailsTestCases.get(2).getEstimate());
        Assert.assertEquals(0, (long)dbTestRailsTestCases.get(2).getEstimateForecast());
    }
}
