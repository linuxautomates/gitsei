package io.levelops.api.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.services.DevProductivityParentProfileService;
import io.levelops.api.services.DevProductivityProfileService;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
public class DevProductivityReportUtils {
    public static final Long DEV_PRODUCTIVITY_REPORT_CACHE_VALUE = 1L;
    public static final TimeUnit DEV_PRODUCTIVITY_REPORT_CACHE_TIME_UNIT = TimeUnit.DAYS;

    public static List<DevProductivityProfile> getDevProductivityProfile(final DevProductivityProfileService devProductivityProfileService, final String company, DefaultListRequest filter, List<Integer> ouRefIds) throws NotFoundException, SQLException, BadRequestException {
        String devProductivityProfileId = filter.getFilterValue("dev_productivity_profile_id", String.class).orElse(null);
        if(StringUtils.isNotBlank(devProductivityProfileId)) {
            DevProductivityProfile devProductivityProfile = devProductivityProfileService.get(company, devProductivityProfileId).orElseThrow(() -> new NotFoundException(String.format("Company %s, Dev Productivity Profile Id %s not found!", company, devProductivityProfileId)));
            log.debug("devProductivityProfile = {}", devProductivityProfile);
            return List.of(devProductivityProfile);
        }else if(CollectionUtils.isNotEmpty(ouRefIds)){
            return devProductivityProfileService.listByFilter(company,DefaultListRequest.builder()
                    .filter(Map.of("ou_ref_ids",ouRefIds)).build()).getRecords();
        }else{
            return devProductivityProfileService.listByFilter(company,DefaultListRequest.builder().build()).getRecords();
        }
    }

    public static List<DevProductivityParentProfile> getDevProductivityParentProfile(final DevProductivityParentProfileService devProductivityParentProfileService, final String company, DefaultListRequest filter, List<Integer> ouRefIds) throws NotFoundException, SQLException, BadRequestException {
        String devProductivityParentProfileId = filter.getFilterValue("dev_productivity_parent_profile_id", String.class).orElse(null);
        if(StringUtils.isNotBlank(devProductivityParentProfileId)) {
            DevProductivityParentProfile devProductivityParentProfile = devProductivityParentProfileService.get(company, devProductivityParentProfileId).orElseThrow(() -> new NotFoundException(String.format("Company %s, Dev Productivity Parent Profile Id %s not found!", company, devProductivityParentProfileId)));
            log.debug("devProductivityParentProfile = {}", devProductivityParentProfile);
            return List.of(devProductivityParentProfile);
        }else if(CollectionUtils.isNotEmpty(ouRefIds)){
            return devProductivityParentProfileService.listByFilter(company,DefaultListRequest.builder()
                    .filter(Map.of("ou_ref_ids",ouRefIds)).build()).getRecords();
        }else{
            return devProductivityParentProfileService.listByFilter(company,DefaultListRequest.builder().build()).getRecords();
        }
    }
}
