package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class BitbucketPaginatedResponse<T> {

    @JsonProperty("size")
    Integer size;
    @JsonProperty("page")
    Integer page;
    @JsonProperty("pagelen")
    Integer pagelen;
    @JsonProperty("next")
    String next;
    @JsonProperty("previous")
    String previous;

    @JsonProperty("values")
    List<T> values;

    public static <T> JavaType ofType(ObjectMapper objectMapper, Class<T> clazz) {
        return objectMapper.getTypeFactory().constructParametricType(BitbucketPaginatedResponse.class, clazz);
    }

    public Optional<String> extractNextPage() {
        if (StringUtils.isEmpty(next)) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = URI.create(next);
        } catch(IllegalArgumentException e) {
            return Optional.empty();
        }
        return URLEncodedUtils.parse(uri, StandardCharsets.UTF_8.toString())
                .stream()
                .filter(kv -> kv.getName().equals("page"))
                .map(NameValuePair::getValue)
                .findAny();
    }

}
