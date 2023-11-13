package io.levelops.commons.databases.models.database.checkmarx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import io.levelops.integrations.checkmarx.models.CxSastScanStatus;
import io.levelops.integrations.checkmarx.models.DateAndTime;
import io.levelops.integrations.checkmarx.models.LanguageState;
import io.levelops.integrations.checkmarx.models.ScanState;
import io.levelops.integrations.checkmarx.models.ScanType;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.ListUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbCxSastScan.DbCxSastScanBuilder.class)
public class DbCxSastScan {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("scan_id")
    String scanId;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("status")
    String status;

    @JsonProperty("scan_type")
    String scanType;

    @JsonProperty("scan_started_at")
    Long scanStartedAt;

    @JsonProperty("scan_finished_at")
    Long scanFinishedAt;

    @JsonProperty("scan_path")
    String scanPath;

    @JsonProperty("languages")
    List<String> languages;

    @JsonProperty("owner")
    String owner;

    @JsonProperty("initiator_name")
    String initiatorName;

    @JsonProperty("is_public")
    boolean isPublic;

    @JsonProperty("scan_risk")
    Integer scanRisk;

    public static DbCxSastScan fromScan(CxSastScan source,
                                        String integrationId) {
        List<LanguageState> languageStates = Optional.of(source.getScanState())
                .map(ScanState::getLanguageStateList)
                .orElse(List.of());
        List<String> languages = ListUtils.emptyIfNull(languageStates.stream()
                .map(lang -> Optional.ofNullable(lang.getLanguageName())
                        .orElse("")).collect(Collectors.toList()));

        return DbCxSastScan.builder()
                .integrationId(integrationId)
                .projectId(source.getProject().getId())
                .scanId(source.getId())
                .status(Optional.of(source.getStatus()).map(CxSastScanStatus::getName).orElse(""))
                .scanType(Optional.ofNullable(source.getScanType()).map(ScanType::getValue).orElse(""))
                .scanStartedAt(Optional.ofNullable(source.getDateAndTime())
                        .map(DateAndTime::getStartedOn)
                        .map(startedOn -> startedOn.toInstant().getEpochSecond())
                        .orElse(new Date().toInstant().getEpochSecond()))
                .scanFinishedAt(Optional.ofNullable(source.getDateAndTime())
                        .map(DateAndTime::getFinishedOn)
                        .map(finishedOn -> finishedOn.toInstant().getEpochSecond())
                        .orElse(new Date().toInstant().getEpochSecond()))
                .scanPath(Optional.of(source.getScanState())
                        .map(ScanState::getPath)
                        .orElse(""))
                .languages(languages)
                .owner(Optional.ofNullable(source.getOwner()).orElse(""))
                .initiatorName(Optional.ofNullable(source.getInitiatorName()).orElse(""))
                .isPublic(Optional.ofNullable(source.getIsPublic()).orElse(false))
                .scanRisk(Optional.ofNullable(source.getScanRisk()).orElse(0))
                .build();
    }
}
