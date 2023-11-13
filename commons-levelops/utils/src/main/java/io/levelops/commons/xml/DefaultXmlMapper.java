package io.levelops.commons.xml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;

/**
 * XML Mapper.
 *
 * (NB: readTree is currently not supported because of multiple same-name XML elements)
 */
public class DefaultXmlMapper {

    private static XmlMapper xmlMapper = null;

    public static XmlMapper getXmlMapper() {
        if (xmlMapper != null) {
            return xmlMapper;
        }
        synchronized (DefaultObjectMapper.class) {
            if (xmlMapper != null) {
                return xmlMapper;
            }
            xmlMapper = buildXmlMapper();
            return xmlMapper;
        }
    }

    private static XmlMapper buildXmlMapper() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        XmlMapper mapper = new XmlMapper(xmlModule);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

}
