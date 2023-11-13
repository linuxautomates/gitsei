package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCase;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Log4j2
public class DbSalesforceCaseConverters {
    public static RowMapper<DbSalesforceCase> listRowMapper() {
        return (rs, rowNumber) -> rowMapperBuilder(rs).build();
    }

    public static DbSalesforceCase.DbSalesforceCaseBuilder rowMapperBuilder(ResultSet rs) throws SQLException {
        return DbSalesforceCase.builder()
                .caseId(rs.getString("case_id"))
                .caseNumber(rs.getString("case_number"))
                .integrationId(rs.getString("integration_id"))
                .accountName(rs.getString("account_name"))
                .subject(rs.getString("subject"))
                .contact(rs.getString("contact"))
                .creator(rs.getString("creator"))
                .isClosed(rs.getBoolean("is_closed"))
                .isDeleted(rs.getBoolean("is_deleted"))
                .isEscalated(rs.getBoolean("is_escalated"))
                .origin(rs.getString("origin"))
                .status(rs.getString("status"))
                .type(rs.getString("type"))
                .priority(rs.getString("priority"))
                .reason(rs.getString("reason"))
                .createdAt(rs.getTimestamp("sf_created_at"))
                .lastModifiedAt(rs.getTimestamp("sf_modified_at"))
                .resolvedAt(rs.getTimestamp("resolved_at"))
                .bounces(rs.getInt("bounces"))
                .hops(rs.getInt("hops"))
                .ingestedAt(rs.getTimestamp("ingested_at").toInstant().getEpochSecond());
    }

    public static RowMapper<DbAggregationResult> distinctRowMapper(String key,
                                                                   SalesforceCaseFilter.CALCULATION calc, Optional<String> additionalKey) {
        return (rs, rowNumber) -> {
            if (calc == SalesforceCaseFilter.CALCULATION.case_count)
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .totalCases(rs.getLong("ct"))
                        .build();
            else
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .totalCases(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("percentile_disc"))
                        .build();
        };
    }
}
