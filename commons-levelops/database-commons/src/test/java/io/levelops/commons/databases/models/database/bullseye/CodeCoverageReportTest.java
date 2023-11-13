package io.levelops.commons.databases.models.database.bullseye;

import io.levelops.commons.utils.ResourceUtils;
import io.levelops.commons.xml.DefaultXmlMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CodeCoverageReportTest {
    @Test
    public void testSerialization() throws IOException {
        String xmlString = ResourceUtils.getResourceAsString("bullseye/test.xml");
        CodeCoverageReport codeCoverageReport = DefaultXmlMapper.getXmlMapper().readValue(xmlString,
                CodeCoverageReport.class);
        Assert.assertNotNull(codeCoverageReport);
    }
}