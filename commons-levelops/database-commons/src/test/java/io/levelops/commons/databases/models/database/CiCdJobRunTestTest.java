package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.levelops.commons.databases.models.database.jenkins.JUnitTestReport;
import io.levelops.commons.xml.DefaultXmlMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

public class CiCdJobRunTestTest {
    private final XmlMapper XML_MAPPER = DefaultXmlMapper.getXmlMapper();

    @Test
    public void testTestsSuccessful() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("jenkins_junit/jenkins_junit_successful_1.xml").toURI());
        final JUnitTestReport jUnitTestReport = XML_MAPPER.readValue(testFile,JUnitTestReport.class);
        List<CiCdJobRunTest> ciCdJobRunTests = CiCdJobRunTest.fromJUnitTestSuite(jUnitTestReport, UUID.randomUUID());
        Assert.assertEquals(1, ciCdJobRunTests.size());
        CiCdJobRunTest ciCdJobRunTest = ciCdJobRunTests.get(0);
        Assert.assertEquals(ciCdJobRunTest.getStatus(), CiCdJobRunTest.Status.PASSED);
        Assert.assertNull(ciCdJobRunTest.getErrorDetails());
        Assert.assertNull(ciCdJobRunTest.getErrorStackTrace());
    }

    @Test
    public void testTestsFailed() throws URISyntaxException, IOException {
        File testFile =  new File(this.getClass().getClassLoader().getResource("jenkins_junit/jenkins_junit_failed_1.xml").toURI());
        final JUnitTestReport jUnitTestReport = XML_MAPPER.readValue(testFile,JUnitTestReport.class);
        List<CiCdJobRunTest> ciCdJobRunTests = CiCdJobRunTest.fromJUnitTestSuite(jUnitTestReport, UUID.randomUUID());
        Assert.assertEquals(1, ciCdJobRunTests.size());
        CiCdJobRunTest ciCdJobRunTest = ciCdJobRunTests.get(0);
        Assert.assertEquals(ciCdJobRunTest.getStatus(), CiCdJobRunTest.Status.FAILED);
        Assert.assertEquals(ciCdJobRunTest.getErrorDetails(), "Error");
        //ToDo: VA Fix Later
        /*
        Assert.assertEquals(ciCdJobRunTest.getErrorStackTrace(), "java.lang.RuntimeException: Error\n" +
                "            at com.viraj.maven_test.ServiceTest.testDoWork(ServiceTest.java:10)");
         */
    }
}