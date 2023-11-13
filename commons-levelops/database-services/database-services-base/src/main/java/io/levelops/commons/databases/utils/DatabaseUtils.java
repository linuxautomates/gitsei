package io.levelops.commons.databases.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class DatabaseUtils {

    /**
     * Create enum type. Ignores errors if the type already exists.
     *
     * @param template  jdbc template
     * @param typeName  name of the db type; usually ends with "_t", e.g. "my_type_t"
     * @param enumClass Enum
     */
    public static <E extends Enum<E>> void createEnumType(JdbcTemplate template, String typeName, Class<E> enumClass) {
        try {
            String sql = generateSqlToCreateEnumType(typeName, enumClass);
            log.debug("sql={}", sql);
            template.execute(sql);
            log.info("Created {} enum type", typeName);
        } catch (DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("ERROR: type \"" + typeName + "\" already exists")) {
                log.info("{} enum type already exists", typeName);
                return;
            }
            log.error("Failed to create {} enum type", typeName, e);
        }
    }

    protected static <E extends Enum<E>> String generateSqlToCreateEnumType(String typeName, Class<E> enumClass) {
        return String.format("CREATE TYPE %s AS ENUM (%s)",
                typeName,
                Stream.of(enumClass.getEnumConstants())
                        .map(E::toString)
                        .map(String::toLowerCase)
                        .map(s -> StringUtils.wrap(s, "'"))
                        .collect(Collectors.joining(", ")));
    }

    public static <T> String toSqlArray(Collection<T> collection) {
        if (CollectionUtils.isEmpty(collection)) {
            return "{}";
        }
        return "{" +
                collection.stream()
                        .map(T::toString)
                        .map(str -> {
                            try {
                                return DefaultObjectMapper.get().writeValueAsString(str);
                            } catch (JsonProcessingException e) {
                                log.warn("Failed to serialize array value: {}", str, e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(",")) +
                "}";
    }

    public static <T> Stream<T> fromSqlArray(Array sqlArray, Class<T> baseClass) throws SQLException {
        Object array = (sqlArray != null) ? sqlArray.getArray() : null;
        if (array == null) {
            return Stream.empty();
        }
        return Arrays.stream((Object[]) array).map(baseClass::cast);
    }

    @FunctionalInterface
    public interface ResultSetExtractor<T> {
        T extract(ResultSet rs) throws SQLException;
    }

    public static <T> Optional<T> executeUpdateAndExtractGeneratedField(PreparedStatement statement, ResultSetExtractor<T> extractor) throws SQLException {
        int affectedRows = statement.executeUpdate();
        if (affectedRows <= 0) {
            return Optional.empty();
        }
        try (ResultSet resultSet = statement.getGeneratedKeys()) {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.ofNullable(extractor.extract(resultSet));
        }
    }

}
