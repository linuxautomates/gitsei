package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserIdentityMaskingService {
    public final static String EXTERNAL_USER = "External User";
    private TenantConfigService tenantConfigService;
    private OrgUsersDatabaseService orgUsersDatabaseService;
    private UserIdentityService userIdentityService;

    @Autowired
    protected UserIdentityMaskingService(DataSource dataSource, TenantConfigService tenantConfigService, OrgUsersDatabaseService orgUsersDatabaseService, UserIdentityService userIdentityService) {
        this.tenantConfigService = tenantConfigService;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
        this.userIdentityService = userIdentityService;
    }

    public static boolean isEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pat.matcher(email).matches();
    }
    public boolean isMasking(String company, String integrationId, String cloudId, String displayName) throws SQLException {
        DbListResponse<TenantConfig> tenantConfigDbListResponse = tenantConfigService.listByFilter(company, "HIDE_EXTERNAL_USER_INFO", 0, 1);
        TenantConfig tenantConfig = tenantConfigDbListResponse.getRecords().size() != 0 ? tenantConfigDbListResponse.getRecords().get(0) : null;
        if (tenantConfig != null && tenantConfig.getValue() != null && Boolean.valueOf(tenantConfig.getValue())) {
            TenantConfig tenantConfig1 = tenantConfigService.listByFilter(company, "SUPPORTED_INTERNAL_DOMAIN", 0, 1).getRecords().get(0);
            if (isEmail(displayName)) {
                String domains = StringUtils.substringAfter(displayName, "@");
                List<String> supportedDomainList = Arrays.stream(tenantConfig1.getValue().split(",")).collect(Collectors.toList());
                if (supportedDomainList.contains(domains)) {
                    return false;
                } else {
                    return true;
                }
            } else {
                Optional<DBOrgUser> orgUser = orgUsersDatabaseService.getByUser(company, displayName);
                if (orgUser.isPresent() && orgUser.filter(s->s.getEmail()!=null).isPresent()) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }
    public String maskedUser(String company){
        return EXTERNAL_USER + userIdentityService.getSequenceNumberForExtUser(company);
    }
}
