package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.util.Strings;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PathSegment.PathSegmentBuilder.class)
public class PathSegment {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("position")
    private Integer position;
    @JsonProperty("type")
    private SegmentType type;

    /**
     * Flip the position of the segments.
     */
    public static Set<PathSegment> reverse(Set<PathSegment> pathSegments) {
        return reindex(toStream(pathSegments, true));
    }

    /**
     * Concatenate 2 sets of segments and re-index the result.
     * @param base
     * @param toAppend
     * @return
     */
    public static Set<PathSegment> concat(Set<PathSegment> base, Set<PathSegment> toAppend) {
        return reindex(
                Stream.concat(
                        toStream(base, false),
                        toStream(toAppend, false)));
    }

    @Nonnull
    public static Set<PathSegment> removeAll(@Nullable Set<PathSegment> base, @Nullable Set<PathSegment> toRemove) {
        Set<String> idsToRemove = SetUtils.emptyIfNull(toRemove).stream()
                .map(PathSegment::getId)
                .collect(Collectors.toSet());
        return reindex(toStream(base, false)
                .filter(pathSegment -> !idsToRemove.contains(pathSegment.getId())));
    }

    /**
     * Relies on the order of the stream to reindex the positions.
     */
    public static Set<PathSegment> reindex(@Nonnull Stream<PathSegment> stream) {
        MutableInt position = new MutableInt(1);
        return stream.map(path -> path.toBuilder()
                .position(position.getAndIncrement())
                .build())
                .collect(Collectors.toSet());
    }

    /**
     * Relies on the position field to sort the stream.
     */
    public static Stream<PathSegment> toStream(@Nullable Set<PathSegment> pathSegments, boolean reversed) {
        if (pathSegments == null) {
            return Stream.empty();
        }
        Comparator<PathSegment> comparator = Comparator.comparingInt((PathSegment path) ->
                MoreObjects.firstNonNull(path.getPosition(), reversed? 0: Integer.MAX_VALUE)); // null values at the end
        if (reversed) {
            comparator = comparator.reversed();
        }
        return pathSegments.stream()
                .filter(Objects::nonNull)
                .sorted(comparator);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PathSegment)) {
            return false;
        }
        var that = (PathSegment) o;
        if (that.id != null && this.id != null) {
            return that.id.toString().equals(this.id.toString());
        }
        if (Strings.isNotBlank(that.name) && that.position != null && Strings.isNotBlank(this.name) && this.position != null) {
            return that.name.equals(this.name) && that.position == this.position;
        }
        return Objects.equals(that.id, this.id) 
                && Objects.equals(that.name, this.name)
                && Objects.equals(that.position, this.position)
                && Objects.equals(that.type, this.type);
    }

    @Override
    public int hashCode() {
        if (this.id != null) {
            return Objects.hash(this.id, this.type);
        }
        if (Strings.isNotBlank(this.name) && this.position != null) {
            return Objects.hash(this.name, this.position, this.type);
        }
        return Objects.hash(this.id, this.name, this.position, this.type);
    }
}