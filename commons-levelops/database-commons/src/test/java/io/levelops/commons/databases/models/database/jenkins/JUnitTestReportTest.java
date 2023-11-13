package io.levelops.commons.databases.models.database.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.levelops.commons.xml.DefaultXmlMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class JUnitTestReportTest {
    private final static XmlMapper MAPPER = DefaultXmlMapper.getXmlMapper();

    @Test
    public void testDeSerialize1() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("jenkins_junit/jenkins_junit_successful_1.xml").toURI());
        final JUnitTestReport jUnitTestReport = MAPPER.readValue(testFile,JUnitTestReport.class);
        Assert.assertNotNull(jUnitTestReport);
        Assert.assertTrue(CollectionUtils.isNotEmpty(jUnitTestReport.getTestCase()));

        JUnitTestReport.TestCase testCase = jUnitTestReport.getTestCase().get(0);
        Assert.assertEquals("com.viraj.maven_test.AppTest", testCase.getClassName());
        Assert.assertEquals("shouldAnswerWithTrue", testCase.getName());
        Assert.assertEquals(0, testCase.getTime().longValue());
    }

    @Test
    public void testDeSerialize2() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("jenkins_junit/jenkins_junit_failed_1.xml").toURI());
        final JUnitTestReport jUnitTestReport = MAPPER.readValue(testFile,JUnitTestReport.class);
        Assert.assertNotNull(jUnitTestReport);
        Assert.assertTrue(CollectionUtils.isNotEmpty(jUnitTestReport.getTestCase()));

        JUnitTestReport.TestCase testCase = jUnitTestReport.getTestCase().get(0);
        Assert.assertEquals("com.viraj.maven_test.ServiceTest", testCase.getClassName());
        Assert.assertEquals("testDoWork", testCase.getName());
        Assert.assertEquals(Float.valueOf(0.007f), testCase.getTime(), 0);

        JUnitTestReport.Error error = testCase.getError();
        Assert.assertEquals("Error", error.getMessage());
        Assert.assertEquals("java.lang.RuntimeException", error.getType());
        Assert.assertEquals("java.lang.RuntimeException: Error\n" +
                "            at com.viraj.maven_test.ServiceTest.testDoWork(ServiceTest.java:10)\n" +
                "        ", error.getFailure());

    }

    @Test
    public void testDeSerialize3() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("jenkins_junit/jenkins_junit_weird_timetamp_LEV-2198.xml").toURI());
        final JUnitTestReport jUnitTestReport = MAPPER.readValue(testFile,JUnitTestReport.class);
        Assert.assertNotNull(jUnitTestReport);
        Assert.assertTrue(CollectionUtils.isNotEmpty(jUnitTestReport.getTestCase()));

        JUnitTestReport.TestCase testCase = jUnitTestReport.getTestCase().get(0);
        Assert.assertEquals("com.io.levelops.configobject.schema.ApplicationEventFilterTest", testCase.getClassName());
        Assert.assertEquals("testGetFilterName", testCase.getName());
        Assert.assertEquals(Float.valueOf(0.000f), testCase.getTime(), 0);

        JUnitTestReport.Error error = testCase.getError();
        Assert.assertNull(error);
    }

    @Test
    public void testDeSerialize4() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("jenkins_junit/jenkins_junit_failed_2.xml").toURI());
        final JUnitTestReport jUnitTestReport = MAPPER.readValue(testFile,JUnitTestReport.class);
        Assert.assertNotNull(jUnitTestReport);
        Assert.assertTrue(CollectionUtils.isNotEmpty(jUnitTestReport.getTestCase()));

        Assert.assertEquals("Mocha Tests", jUnitTestReport.getName());
        Assert.assertEquals(1, jUnitTestReport.getTests().intValue());
        Assert.assertEquals(0, jUnitTestReport.getSkipped().intValue());
        Assert.assertEquals(1, jUnitTestReport.getFailures().intValue());
        Assert.assertEquals(1, jUnitTestReport.getErrors().intValue());
        Assert.assertEquals(Float.valueOf(3.787F), jUnitTestReport.getTime(), 0);

        JUnitTestReport.TestCase testCase = jUnitTestReport.getTestCase().get(0);
        Assert.assertEquals("/workspace/webapp/test/unit-tests/lib", testCase.getClassName());
        Assert.assertEquals("/workspace/webapp/test/unit-tests/lib/batch-executor-stages/create-ingest-job-test.js", testCase.getName());
        Assert.assertEquals(Float.valueOf(0.000f), testCase.getTime(), 0);

        Assert.assertNull(testCase.getError());

        JUnitTestReport.Error failure = testCase.getFailure();
        Assert.assertEquals(null, failure.getMessage());
        Assert.assertEquals(null, failure.getType());
        Assert.assertNotNull(failure.getFailure());
    }

    @Test
    public void serialize() throws JsonProcessingException {
        JUnitTestReport.Error expected = JUnitTestReport.Error.builder()
                .type("java.lang.RuntimeException")
                .message("Error")
                .failure("failure msg")
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        Assert.assertNotNull(serialized);
        JUnitTestReport.Error actual = MAPPER.readValue(serialized, JUnitTestReport.Error.class);
        //ToDo: VA Fix this
        //Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTestcasesSkipped() throws IOException, URISyntaxException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("jenkins_junit/jenkins_junit_testcases_skipped.xml").toURI());
        final JUnitTestReport jUnitTestReport = MAPPER.readValue(testFile,JUnitTestReport.class);
        Assert.assertNotNull(jUnitTestReport);
        Assert.assertEquals(jUnitTestReport.getTestCase().get(0).getSkipped(), "");
        // Assert.assertNull(jUnitTestReport.getTestCase().get(0).getSkipped());
        Assert.assertNotNull(jUnitTestReport.getTestCase().get(1).getSkipped());
        Assert.assertNotNull(jUnitTestReport.getTestCase().get(2).getSkipped());
        Assert.assertNotNull(jUnitTestReport.getTestCase().get(3).getSkipped());
    }
}