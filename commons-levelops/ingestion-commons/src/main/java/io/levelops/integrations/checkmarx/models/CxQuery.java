package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CxQuery {

    @JsonProperty("id")
    String id;

    @JsonProperty("categories")
    String categories;

    @Setter
    @JsonProperty("category")
    List<String> category;

    @JsonProperty("description")
    List<String> description;

    @JsonProperty("cweId")
    String cweId;

    @JsonProperty("name")
    String name;

    @JsonProperty("group")
    String group;

    @JsonProperty("Severity")
    String severity;

    @JsonProperty("Language")
    String language;

    @JsonProperty("LanguageHash")
    String languageHash;

    @JsonProperty("SeverityIndex")
    String severityIndex;

    @JsonProperty("LanguageChangeDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    Date languageChangeDate;

    @JsonProperty("QueryPath")
    String queryPath;

    @JsonProperty("QueryVersionCode")
    String queryVersionCode;

    @JsonProperty("results")
    @JacksonXmlProperty(localName = "Result")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<CxResult> results; // children
}
