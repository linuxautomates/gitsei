package io.levelops.faceted_search;

import io.levelops.web.exceptions.BadRequestException;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum IssueLinkageType {
    RELATES_TO ( List.of("relates to")),
    DUPLICATES (List.of("duplicates")),
    IS_DUPLICATED_BY (List.of("is duplicated by")),
    IS_BLOCKED_BY (List.of("is blocked by")),
    BLOCKS (List.of("blocks"));

    private static final EnumMap<IssueLinkageType, List<IssueLinkageType>> LINKAGE_MAP = new EnumMap<>(IssueLinkageType.class);

    static {
        LINKAGE_MAP.put(RELATES_TO, List.of(RELATES_TO));
        LINKAGE_MAP.put(DUPLICATES, List.of(IS_DUPLICATED_BY));
        LINKAGE_MAP.put(IS_DUPLICATED_BY, List.of(DUPLICATES));
        LINKAGE_MAP.put(IS_BLOCKED_BY, List.of(BLOCKS));
        LINKAGE_MAP.put(BLOCKS, List.of(IS_BLOCKED_BY));
    }

    private List<String> links;
    IssueLinkageType(List<String> links) {
        this.links = links;
    }
    public static IssueLinkageType fromString(String str) throws BadRequestException {
        for (IssueLinkageType linkageType : IssueLinkageType.values()) {
            if (linkageType.getLinks().contains(str)) {
                return linkageType;
            }
        }
        throw new BadRequestException("Unknown linkage type detected "+str);
    }
    public static List<IssueLinkageType> fromStringList(List<String> values) {
        return values.stream().map( v -> {
            try {
                return fromString(v);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public List<String> getRelatedStringTypes(){
        return LINKAGE_MAP.get(this).stream().flatMap(il -> il.getLinks().stream()).collect(Collectors.toList());
    }
    public List<IssueLinkageType> getRelatedTypes() {
        return LINKAGE_MAP.get(this);
    }
}

