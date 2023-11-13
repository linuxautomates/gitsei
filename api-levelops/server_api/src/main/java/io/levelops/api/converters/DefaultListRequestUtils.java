package io.levelops.api.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.utils.MapUtilsForRESTControllers;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class DefaultListRequestUtils {

    public static List<String> getListOrDefault(final Map<String, Object> filter, final String key){
        return MapUtilsForRESTControllers.getListOrDefault(filter, key);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getListOfObjectOrDefault(Map<String, Object> filter, String key) {
        try {
            return (List<Object>) filter.getOrDefault(key, Collections.emptyList());
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: " + key);
        }
    }

    public static List<CiCdJobRunParameter> parseCiCdJobRunParameters(ObjectMapper objectMapper, List<Object> parameterObjects) {
        log.debug("parameterObjects = {}", parameterObjects);
        if(CollectionUtils.isEmpty(parameterObjects)){
            return Collections.emptyList();
        }
        try {
            String serialized = objectMapper.writeValueAsString(parameterObjects);
            log.debug("serialized = {}", serialized);
            List<CiCdJobRunParameter> parameters = objectMapper.readValue(serialized, objectMapper.getTypeFactory().constructCollectionType(List.class, CiCdJobRunParameter.class));
            log.debug("parameters = {}", parameters);
            return parameters;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: parameters");
        }
    }

    public static List<CiCdJobQualifiedName> parseCiCdQualifiedJobNames(ObjectMapper objectMapper, List<Object> qualifiedJobNameObjects) {
        log.debug("qualifiedJobNameObjects = {}", qualifiedJobNameObjects);
        if(CollectionUtils.isEmpty(qualifiedJobNameObjects)){
            return Collections.emptyList();
        }
        try {
            String serialized = objectMapper.writeValueAsString(qualifiedJobNameObjects);
            log.debug("serialized = {}", serialized);
            List<CiCdJobQualifiedName> qualifiedJobNames = objectMapper.readValue(serialized, objectMapper.getTypeFactory().constructCollectionType(List.class, CiCdJobQualifiedName.class));
            log.debug("qualifiedJobNames = {}", qualifiedJobNames);
            return qualifiedJobNames;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: qualified_job_names");
        }
    }

    public static <T extends Enum<T>> List<T> parseCiCdStacks(List<String> stackStrings, Class<T> t){
        if(CollectionUtils.isEmpty(stackStrings)) {
            return Collections.emptyList();
        }
        List<T> results = stackStrings.stream().map(x -> EnumUtils.getEnumIgnoreCase(t, x)).collect(Collectors.toList());
        log.info("stacks = {}", results);
        return results;
    }
}
