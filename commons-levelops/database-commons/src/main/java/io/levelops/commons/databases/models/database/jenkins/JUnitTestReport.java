package io.levelops.commons.databases.models.database.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/*
See issue:
This is due to https://github.com/FasterXML/jackson-dataformat-xml/issues/306
JacksonXmlText does NOT deserialize data.
It does not play well with Value + JsonDeserialize
Xml Object Mapper inherits from ObjectMapper, so if we use Json Properties with Xml Properties, it causes issues.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class JUnitTestReport {
    @JacksonXmlProperty(isAttribute = true)
    String name;

    @JacksonXmlProperty(isAttribute = true)
    Integer tests;

    @JacksonXmlProperty(isAttribute = true)
    Integer skipped;

    @JacksonXmlProperty(isAttribute = true)
    Integer failures;

    @JacksonXmlProperty(isAttribute = true)
    Integer errors;

    // Removed timestamp from de-serialization, field is NOT used & also too many formats so causes errors
    // Do NOT Add timestamp field again - See LEV-2198

    @JacksonXmlProperty(isAttribute = true)
    String hostName;

    @JacksonXmlProperty(isAttribute = true)
    String packageName;

    @JacksonXmlProperty(isAttribute = true)
    Float time;

    @JsonProperty("testcase")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "testcase")
    List<TestCase> testCase;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class TestCase {
        @JacksonXmlProperty(isAttribute = true)
        String name;

        @JacksonXmlProperty(isAttribute = true, localName = "classname")
        String className;

        @JacksonXmlProperty(isAttribute = true)
        Float time;

        @JacksonXmlProperty(isAttribute = true)
        Object skipped;

        @JacksonXmlElementWrapper
        Error failure;

        @JsonProperty("error")
        @JacksonXmlProperty
        Error error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Error {
        @JacksonXmlProperty(isAttribute = true)
        String message;
        @JacksonXmlProperty(isAttribute = true)
        String type;

        @JacksonXmlText(value = true)
        String failure;
    }
}