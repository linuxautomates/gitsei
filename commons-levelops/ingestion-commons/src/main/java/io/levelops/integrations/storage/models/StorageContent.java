package io.levelops.integrations.storage.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.ListResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Use this model to enhance content with metadata before serializing.
 *
 * @param <T>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class StorageContent<T> implements Serializable {

    @JsonProperty("_metadata")
    private StorageMetadata storageMetadata;

    @JsonUnwrapped
    private T data;

    public static <T> JavaType getListStorageContentJavaType(ObjectMapper m, Class<T> clazz) {
        return m.getTypeFactory().constructParametricType(StorageContent.class,
                m.getTypeFactory().constructParametricType(ListResponse.class,
                        clazz));
    }

    public static JavaType getListStorageContentJavaType(ObjectMapper m, JavaType parameterType) {
        return m.getTypeFactory().constructParametricType(StorageContent.class,
                m.getTypeFactory().constructParametricType(ListResponse.class,
                        parameterType));
    }
}