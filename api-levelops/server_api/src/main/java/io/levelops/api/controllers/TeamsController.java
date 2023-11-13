package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeam.TeamMemberId;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/teams")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class TeamsController {
   private final TeamsDatabaseService teamsService;
   private final TeamMembersDatabaseService teamMembersService;

   @Autowired
   public TeamsController(final TeamsDatabaseService teamsService, final TeamMembersDatabaseService teamMembersService) {
       this.teamsService = teamsService;
       this.teamMembersService = teamMembersService;
   }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
   public DeferredResult<ResponseEntity<Map<String, String>>> createTeam(@SessionAttribute("company") final String company, @RequestBody() DBTeam team){
        return SpringUtils.deferResponse(() -> {
            var requestManagers = team.getManagers().stream().collect(Collectors.partitioningBy(item -> item.getId() != null));
            var requestMembers = team.getMembers().stream().collect(Collectors.partitioningBy(item -> item.getId() != null));
            // Get team members/managers without ids
            var membersNoId = new HashSet<TeamMemberId>();
            membersNoId.addAll(requestManagers.get(false));
            membersNoId.addAll(requestMembers.get(false));
            if (membersNoId.size() == 0) {

            }
            // Get ids for team members without ids (by email lookup)
            var membersIds = teamMembersService.filter(
                company,
                QueryFilter.builder()
                    .strictMatch("email", membersNoId.stream().map(item -> item.getEmail()).collect(Collectors.toSet()))
                    .build(),
                    0,
                    membersNoId.size())
                    .getRecords().stream()
                        .collect(Collectors.toMap(DBTeamMember::getEmail, (DBTeamMember item) -> item));
            // Get a full list of managers
            var managerWithIds = new HashSet<TeamMemberId>();
            // Add managers with ids
            managerWithIds.addAll(requestManagers.get(true));
            // Add managers without ids by merging with the ids obtained from the db by email lookup
            managerWithIds.addAll(requestManagers.get(false).stream().map(m -> m.toBuilder().id(membersIds.get(m.getEmail()).getId()).build()).collect(Collectors.toSet()));
            // Get a full list of members
            var membersWithIds = new HashSet<TeamMemberId>();
            // Add team members with ids
            membersWithIds.addAll(requestManagers.get(true));
            // add team members without ids by merging with the ids obtained from the db by email lookup
            membersWithIds.addAll(requestManagers.get(false).stream().map(m -> m.toBuilder().id(membersIds.get(m.getEmail()).getId()).build()).collect(Collectors.toSet()));

            // if any member/manager was not found in the db we will insert them as new team members as long as we have email and full name
            if (managerWithIds.size() != team.getManagers().size() || membersWithIds.size() != team.getMembers().size()) {
                var newMembers = new HashSet<TeamMemberId>();
                // register new team members if full_name and email available
                newMembers.addAll(team.getManagers().stream()
                    .filter(item -> !membersIds.containsKey(item.getEmail()) && StringUtils.isNotBlank(item.getFullName()))
                    .collect(Collectors.toSet()));
                newMembers.addAll(team.getMembers().stream()
                    .filter(item -> !membersIds.containsKey(item.getEmail()) && StringUtils.isNotBlank(item.getFullName()))
                    .collect(Collectors.toSet()));
                // TODO: bulk insert team members and add them to team managers or team members respectively
            }
            var teamComplete = team.toBuilder()
                .managers(managerWithIds)
                .members(membersWithIds)
                .build();
            // insert new team members
            return ResponseEntity.ok(Map.of("id", teamsService.insertAndReturnId(company, teamComplete)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to insert the Team '" + team.getName() + "', please check your values or try again."))));
        });
   }

   private UUID getIdByEmail(final String company, final String email){
       try {
        return teamMembersService.filter(company, QueryFilter.builder().strictMatch("email", email).build(), 0, 1).getRecords().get(0).getId();
    } catch (SQLException e) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "");
    }
   }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
   public DeferredResult<ResponseEntity<DBTeam>> getTeam(@SessionAttribute("company") String company, @PathVariable("id") UUID teamId){
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
            teamsService.get(company, teamId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team with id '" + teamId.toString() + "' not found"))));
   }

   @DeleteMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
   public DeferredResult<ResponseEntity<String>> deleteTeams(@SessionAttribute("company") String company, @RequestBody() Set<UUID> teamIds){
        return SpringUtils.deferResponse(() -> {
            try {
                teamsService.delete(company, teamIds);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok("OK");
        });
   }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(path = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
   public DeferredResult<ResponseEntity<PaginatedResponse<DBTeam>>> listTeams(@SessionAttribute("company") String company, @RequestBody DefaultListRequest request){
        return SpringUtils.deferResponse(() -> {
            var teams = teamsService.filter(company, QueryFilter.fromRequestFilters(request.getFilter()), request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), teams.getTotalCount(), teams.getRecords()));
        });
   }
}
