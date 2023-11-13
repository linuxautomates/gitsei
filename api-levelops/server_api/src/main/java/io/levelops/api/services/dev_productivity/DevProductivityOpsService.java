package io.levelops.api.services.dev_productivity;

import io.levelops.api.converters.DefaultListRequestUtils;
import io.levelops.auth.auth.config.Auth;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.dev_productivity.services.DevProdTaskReschedulingService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
@Service
public class DevProductivityOpsService {
    private final Set<String> TENANTS_AUTHORIZED_TO_RESCHEDULE_REPORTS = Set.of("levelops", "foo");
    private final Set<String> USERS_AUTHORIZED_TO_RESCHEDULE_REPORTS = Set.of(
            "viraj@levelops.io", "viraj@propelo.ai",
            "maxime@levelops.io", "maxime@propelo.ai",
            "ivan@levelops.io", "ivan@propelo.ai",
            "ashish@levelops.io", "ashish@propelo.ai",
            "siva@levelops.io", "siva@propelo.ai",
            "satish@levelops.io", "satish@propelo.ai"
    );

    private final UserService userService;
    private final DevProdTaskReschedulingService devProdTaskReschedulingService;

    private final Auth auth;

    @Autowired
    public DevProductivityOpsService(UserService userService, DevProdTaskReschedulingService devProdTaskManagementService, Auth auth) {
        this.userService = userService;
        this.devProdTaskReschedulingService = devProdTaskManagementService;
        this.auth = auth;
    }

    private boolean verifyRequestorHasCorrectScopes(final Map<String, List<String>> requestorsScopes) {
        if( (MapUtils.isNotEmpty(requestorsScopes) && requestorsScopes.containsKey("dev_productivity_write")) || !auth.isLegacy()) {
            return true;
        }
        return false;
    }

    private User assignDevProdScope(final String company, final String requestorEmail, final Map<String, List<String>> requestorsScopes, final String targetEmail) throws ForbiddenException, SQLException, NotFoundException {

        //Check if target user exists
        Optional<User> optionalUser = userService.getByEmail(company, targetEmail);
        if(optionalUser.isEmpty()) {
            String error = String.format("Target user %s does not exist, cannot assign dev prod scope.", targetEmail);
            throw new NotFoundException(error);
        }

        //Update existing scopes with dev prod scope
        Map<String, List<String>> updatedScopes = new HashMap<>();
        for(Map.Entry<String, List<String>> e : MapUtils.emptyIfNull(optionalUser.get().getScopes()).entrySet()) {
            updatedScopes.put(e.getKey(), e.getValue());
        }
        updatedScopes.put("dev_productivity_write", List.of());
        log.info("updatedScopes = {}", updatedScopes);

        //Update target users scopes with dev prod scope
        String userId = optionalUser.get().getId();
        userService.updateScopes(company, userId, updatedScopes);

        //Return updated user
        Optional<User> optionalUpdatedUser = userService.getByEmail(company, targetEmail);
        if(optionalUpdatedUser.isEmpty()) {
            String error = String.format("Target user %s does not exist, cannot assign dev prod scope.", targetEmail);
            throw new NotFoundException(error);
        }
        return optionalUpdatedUser.get();
    }

    public List<User> assignDevProdScope(final String company, final String requestorEmail, final Map<String, List<String>> requestorsScopes, final DefaultListRequest filter) throws ForbiddenException, SQLException, NotFoundException {
        List<String> targetEmails = DefaultListRequestUtils.getListOrDefault(filter.getFilter(), "target_emails");
        List<User> users = new ArrayList<>();
        for(String targetEmail : CollectionUtils.emptyIfNull(targetEmails)) {
            users.add(assignDevProdScope(company, requestorEmail, requestorsScopes, targetEmail));
        }
        return users;
    }

    private User removeDevProdScope(final String company, final String requestorEmail, final Map<String, List<String>> requestorsScopes, final String targetEmail) throws ForbiddenException, SQLException, NotFoundException {
        //Check if target user exists
        Optional<User> optionalUser = userService.getByEmail(company, targetEmail);
        if(optionalUser.isEmpty()) {
            String error = String.format("Target user %s does not exist, cannot assign dev prod scope.", targetEmail);
            throw new NotFoundException(error);
        }

        //remove trellis scopes from user
        Map<String,List<String>> updatedScopes = optionalUser.get().getScopes();
        if((MapUtils.isNotEmpty(updatedScopes) && updatedScopes.containsKey("dev_productivity_write")) || !auth.isLegacy()){
            updatedScopes.remove("dev_productivity_write");
        }

        log.info("updatedScopes = {}", updatedScopes);

        //Update target users scopes with dev prod scope
        String userId = optionalUser.get().getId();
        userService.updateScopes(company, userId, updatedScopes);

        //Return updated user
        Optional<User> optionalUpdatedUser = userService.getByEmail(company, targetEmail);
        if(optionalUpdatedUser.isEmpty()) {
            String error = String.format("Target user %s does not exist, cannot assign dev prod scope.", targetEmail);
            throw new NotFoundException(error);
        }
        return optionalUpdatedUser.get();
    }

    public List<User> removeDevProdScope(String company, String requestorEmail, Map<String, List<String>> requestorsScopes, DefaultListRequest filter) throws ForbiddenException, SQLException, NotFoundException {
        List<String> targetEmails = DefaultListRequestUtils.getListOrDefault(filter.getFilter(), "target_emails");
        List<User> users = new ArrayList<>();
        for(String targetEmail : CollectionUtils.emptyIfNull(targetEmails)) {
            users.add(removeDevProdScope(company, requestorEmail, requestorsScopes, targetEmail));
        }
        return users;
    }

    public Boolean reScheduleReportSingleTenant(final String company, final String requestorEmail, final Map<String, List<String>> requestorsScopes, final DefaultListRequest filter) throws ForbiddenException {
        //First check if requestor has dev prod scope
        if (! verifyRequestorHasCorrectScopes(requestorsScopes)) {
            String error = String.format("Requestor %s does not have dev prod scope, cannot reschedule report.", requestorEmail);
            throw new ForbiddenException(error);
        }
        return devProdTaskReschedulingService.reScheduleReportForOneTenant(company);
    }

    public List<String> reScheduleReportAllTenants(final String company, final String requestorEmail, final Map<String, List<String>> requestorsScopes, final DefaultListRequest filter) throws ForbiddenException {
        //First check if requestor has dev prod scope
        if (! USERS_AUTHORIZED_TO_RESCHEDULE_REPORTS.contains(requestorEmail) || !TENANTS_AUTHORIZED_TO_RESCHEDULE_REPORTS.contains(company)) {
            String error = String.format("Requestor %s & customer %s does not have permissions to reschedule report for all tenants.", requestorEmail, company);
            throw new ForbiddenException(error);
        }
        List<String> requestedTenants = getListOrDefault(filter, "tenants");
        log.info("requestedTenants = {}", requestedTenants);
        List<String> tenantIds = devProdTaskReschedulingService.reScheduleReportForMultipleTenants(requestedTenants);
        log.info("rescheduled reports for tenants, {}", tenantIds);
        return tenantIds;
    }

}
