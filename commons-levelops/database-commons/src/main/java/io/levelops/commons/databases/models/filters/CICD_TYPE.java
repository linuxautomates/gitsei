package io.levelops.commons.databases.models.filters;

import io.levelops.commons.models.DefaultListRequest;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

public enum CICD_TYPE {
    jenkins,
    azure_devops,
    droneci,
    harnessng,
    circleci,
    gitlab,
    github_actions;

    public static CICD_TYPE fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(CICD_TYPE.class, st);
    }


    public static List<CICD_TYPE> parseFromFilter(DefaultListRequest filter) {
        try {
           return parseFromFilter(filter.getFilter());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid 'type' provided.");
        }
    }

    public static List<CICD_TYPE> parseFromFilter(Map<String, Object> filter){
        try {
            return getListOrDefault(filter, "types").stream()
                    .map(CICD_TYPE::valueOf)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid 'type' provided.");
        }
    }

}


