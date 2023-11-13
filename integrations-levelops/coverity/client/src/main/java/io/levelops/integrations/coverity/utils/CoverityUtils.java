package io.levelops.integrations.coverity.utils;

import com.coverity.ws.v9.StreamDataObj;
import com.coverity.ws.v9.StreamIdDataObj;
import org.apache.commons.collections4.CollectionUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

public class CoverityUtils {

    public static List<StreamIdDataObj> extractIdsFromStreams(List<StreamDataObj> streams) {
        if (CollectionUtils.isNotEmpty(streams))
            return streams.stream().map(StreamDataObj::getId).collect(Collectors.toList());
        return List.of();
    }

    public static XMLGregorianCalendar convertDate(Date date) {
        try {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(date);
            return DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(gc);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
