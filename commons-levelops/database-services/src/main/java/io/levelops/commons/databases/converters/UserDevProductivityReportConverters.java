package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.dev_productivity.SectionResponse;
import io.levelops.commons.databases.models.database.dev_productivity.UserDevProductivityReport;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.core.RowMapper;

import java.nio.file.attribute.FileAttribute;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class UserDevProductivityReportConverters {
    private static final boolean RESPONSE_IS_V1 = true;
    public static final boolean RESPONSE_IS_NOT_V1 = false;

    public static RowMapper<UserDevProductivityReport> rowMapper(ObjectMapper objectMapper, boolean needRawResponse) {
        return rowMapper(objectMapper, needRawResponse, RESPONSE_IS_V1);
    }

    public static RowMapper<UserDevProductivityReport> rowMapper(ObjectMapper objectMapper, boolean needRawResponse, boolean isResponseV1) {
        return (rs, rowNumber) -> {
            DevProductivityResponse originalReport = ParsingUtils.parseObject(objectMapper, "report", DevProductivityResponse.class, rs.getString("report"));
            DevProductivityResponse sanitizedReport = originalReport.toBuilder()
                    .sectionResponses(CollectionUtils.isNotEmpty(originalReport.getSectionResponses()) ?
                            originalReport.getSectionResponses().stream().filter(sr -> BooleanUtils.isNotFalse(sr.getEnabled()))
                            .map(sr -> SectionResponse.builder()
                                    .name(sr.getName())
                                    .description(sr.getDescription())
                                    .order(sr.getOrder())
                                    .enabled(sr.getEnabled())
                                    .score(sr.getScore())
                                    .weightedScore(sr.getWeightedScore())
                                    .featureResponses(CollectionUtils.isNotEmpty(sr.getFeatureResponses()) ?
                                            sr.getFeatureResponses().stream().filter(fr -> BooleanUtils.isNotFalse(fr.getEnabled())).collect(Collectors.toList()) : null).build())
                            .collect(Collectors.toList()) : null)
                    .build();
            Instant devProductivityProfileTimestamp = null;
            Boolean latest = null;
            UUID requestedOrgUserId = null;
            Boolean incomplete = null;
            List<String> missingFeatures = new ArrayList<>();

            if(isResponseV1) {
                devProductivityProfileTimestamp = DateUtils.toInstant(rs.getTimestamp("dev_productivity_profile_timestamp"));
            } else {
                latest = rs.getBoolean("latest");
                requestedOrgUserId = (UUID)rs.getObject("requested_org_user_id");
                incomplete = rs.getBoolean("incomplete");
                missingFeatures = (rs.getArray("missing_features") != null && rs.getArray("missing_features").getArray() != null)
                        ? Arrays.asList((String[]) rs.getArray("missing_features").getArray()) : List.of();
            }
            return UserDevProductivityReport.builder()
                    .id((UUID)rs.getObject("id"))
                    .orgUserId((UUID)rs.getObject("org_user_id"))
                    .orgUserRefId(rs.getInt("org_user_ref_id"))
                    .devProductivityProfileId((UUID)rs.getObject("dev_productivity_profile_id"))
                    .devProductivityProfileTimestamp(devProductivityProfileTimestamp)
                    .interval(ReportIntervalType.fromString(rs.getString("interval")))
                    .startTime(DateUtils.toInstant(rs.getTimestamp("start_time")))
                    .endTime(DateUtils.toInstant(rs.getTimestamp("end_time")))
                    .weekOfYear(columnPresent(rs,"week_of_year") ? rs.getInt("week_of_year") : -1)
                    .year(columnPresent(rs,"year") ? rs.getInt("year") : -1)
                    .latest(latest)
                    .score(rs.getInt("score"))
                    .report(needRawResponse ? originalReport : sanitizedReport)
                    .requestedOrgUserId(requestedOrgUserId)
                    .incomplete(incomplete)
                    .missingFeatures(missingFeatures)
                    .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                    .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                    .build();
        };
    }
    private static boolean columnPresent(ResultSet rs, String column) {
        boolean isColumnPresent = false;
        try {
            rs.findColumn(column);
            if (ObjectUtils.isNotEmpty(rs.getObject(column))) {
                isColumnPresent = true;
            }
        } catch (SQLException e) {
            isColumnPresent = false;
        }
        return isColumnPresent;
    }
}
