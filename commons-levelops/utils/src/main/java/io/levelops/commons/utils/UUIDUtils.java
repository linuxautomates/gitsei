package io.levelops.commons.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UUIDUtils {
    public static UUID fromString (final String input){
        return (StringUtils.isBlank(input))? null: UUID.fromString(input);
    }
    public static List<UUID> fromStringsList(final List<String> input){
        if(CollectionUtils.isEmpty(input)){
            return Collections.emptyList();
        }
        return input.stream().map(UUIDUtils::fromString).collect(Collectors.toList());
    }
    public static List<String> toStringsList(final List<UUID> input){
        if(CollectionUtils.isEmpty(input)){
            return Collections.emptyList();
        }
        return input.stream().map(UUID::toString).collect(Collectors.toList());
    }
}
