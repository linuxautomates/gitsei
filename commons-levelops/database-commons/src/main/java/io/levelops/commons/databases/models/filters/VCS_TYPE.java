package io.levelops.commons.databases.models.filters;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

public enum VCS_TYPE {
    GIT,
    TFVC,
    PERFORCE;

    public static VCS_TYPE fromString(String st) {
        return EnumUtils.getEnumIgnoreCase(VCS_TYPE.class, st);
    }

    public static List<VCS_TYPE> parseFromFilter(Map<String, Object> filter) {
        try {
            return getListOrDefault(filter, "vcs_types").stream()
                    .map(VCS_TYPE::valueOf)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid 'vcs_type' provided.");
        }
    }
}
