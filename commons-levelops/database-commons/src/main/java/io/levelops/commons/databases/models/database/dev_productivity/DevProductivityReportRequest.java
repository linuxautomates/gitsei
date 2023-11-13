package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DevProductivityReportRequest.DevProductivityReportRequestBuilder.class)
public class DevProductivityReportRequest {

    @JsonProperty("report_requests")
    private List<DevProductivityUserIds> devProductivityUserIds;

    public static DevProductivityReportRequest fromListRequest(DefaultListRequest filter) throws BadRequestException {

       List<DevProductivityUserIds> list = parseAllIds(new ObjectMapper(), filter.getFilterValueAsList("report_requests").get());
       return DevProductivityReportRequest.builder()
               .devProductivityUserIds(list)
               .build();
    }

    public static List<DevProductivityUserIds> parseAllIds(ObjectMapper objectMapper, List<Object> idObjects) throws BadRequestException {

        if(CollectionUtils.isEmpty(idObjects)){
            return Collections.emptyList();
        }
        try {
            String serialized = objectMapper.writeValueAsString(idObjects);
            List<DevProductivityUserIds> objects = objectMapper.readValue(serialized, objectMapper.getTypeFactory().constructCollectionType(List.class, DevProductivityUserIds.class));
            return objects;
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid filter parameter: report_requests");
        }
    }
}


