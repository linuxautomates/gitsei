package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.OrgDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.dev_productivity.SectionResponse;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class OrgDevProductivityReportsConverters {
    private static final boolean RESPONSE_IS_V1 = true;
    public static final boolean RESPONSE_IS_NOT_V1 = false;

    public static RowMapper<OrgDevProductivityReport> rowMapper(ObjectMapper objectMapper, boolean needRawReport) {
        return rowMapper(objectMapper,needRawReport, RESPONSE_IS_V1);
    }

    public static RowMapper<OrgDevProductivityReport> rowMapper(ObjectMapper objectMapper, boolean needRawReport, boolean isResponseV1) {
        return (rs, rowNumber) -> {
            DevProductivityResponse originalReport = ParsingUtils.parseObject(objectMapper, "report", DevProductivityResponse.class, rs.getString("report"));
            DevProductivityResponse sanitizedReport = originalReport.toBuilder()
                    .sectionResponses(CollectionUtils.isNotEmpty(originalReport.getSectionResponses()) ?
                            originalReport.getSectionResponses().stream().filter(sr -> Boolean.TRUE.equals(sr.getEnabled()))
                                    .map(sr -> SectionResponse.builder()
                                            .name(sr.getName())
                                            .description(sr.getDescription())
                                            .order(sr.getOrder())
                                            .enabled(sr.getEnabled())
                                            .score(sr.getScore())
                                            .weightedScore(sr.getWeightedScore())
                                            .featureResponses(CollectionUtils.isNotEmpty(sr.getFeatureResponses()) ?
                                                    sr.getFeatureResponses().stream().filter(fr -> Boolean.TRUE.equals(fr.getEnabled())).collect(Collectors.toList()) : null).build())
                                    .collect(Collectors.toList()) : null)
                    .build();

            Instant devProductivityProfileTimestamp = null;
            Boolean latest = null;
            UUID requestedOUId = null;
            Integer missingUserReportsCount = null;
            Integer staleUserReportsCount = null;
            if(isResponseV1) {
                devProductivityProfileTimestamp = DateUtils.toInstant(rs.getTimestamp("dev_productivity_profile_timestamp"));
            } else {
                latest = rs.getBoolean("latest");
                requestedOUId = (UUID)rs.getObject("requested_ou_id");
                missingUserReportsCount = rs.getInt("missing_user_reports_count");
                staleUserReportsCount = rs.getInt("stale_user_reports_count");
            }

            return OrgDevProductivityReport.builder()
                    .id((UUID)rs.getObject("id"))
                    .ouID((UUID)rs.getObject("ou_id"))
                    .ouRefId(rs.getInt("ou_ref_id"))
                    .devProductivityProfileId(columnPresent(rs,"dev_productivity_profile_id") ? (UUID)rs.getObject("dev_productivity_profile_id") : null)
                    .devProductivityParentProfileId(columnPresent(rs,"dev_productivity_parent_profile_id") ? (UUID)rs.getObject("dev_productivity_parent_profile_id") : null)
                    .devProductivityProfileTimestamp(devProductivityProfileTimestamp)
                    .interval(ReportIntervalType.fromString(rs.getString("interval")))
                    .startTime(DateUtils.toInstant(rs.getTimestamp("start_time")))
                    .endTime(DateUtils.toInstant(rs.getTimestamp("end_time")))
                    .weekOfYear(columnPresent(rs,"week_of_year") ? rs.getInt("week_of_year") : -1)
                    .year(columnPresent(rs,"year") ? rs.getInt("year") : -1)
                    .latest(latest)
                    .score(rs.getInt("score"))
                    .report(needRawReport ? originalReport : sanitizedReport)
                    .requestedOUId(requestedOUId)
                    .missingUserReportsCount(missingUserReportsCount)
                    .staleUserReportsCount(staleUserReportsCount)
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
