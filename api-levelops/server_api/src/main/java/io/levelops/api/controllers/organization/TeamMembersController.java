package io.levelops.api.controllers.organization;

import com.google.common.base.Preconditions;
import io.levelops.commons.databases.models.database.QueryFilter;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/v1/teams/members")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
public class TeamMembersController {
//    private final TeamsDatabaseService teamsService;
   private final TeamMembersDatabaseService teamMembersService;

   @Autowired
   public TeamMembersController(final TeamsDatabaseService teamsService, final TeamMembersDatabaseService teamMembersService) {
    //    this.teamsService = teamsService;
       this.teamMembersService = teamMembersService;
   }

   @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
   public DeferredResult<ResponseEntity<Map<String, String>>> createTeamMember(@SessionAttribute("company") final String company, @RequestBody() DBTeamMember teamMember){
        return SpringUtils.deferResponse(() -> {
            try{
                Preconditions.checkArgument(teamMember.getIds() != null && teamMember.getIds().size() > 0, "A team member must contain at least one username");
                // insert new team members
                return ResponseEntity.ok(Map.of("id", teamMembersService.insertAndReturnId(company, teamMember)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to insert the Team member'" + StringUtils.firstNonBlank(teamMember.getFullName(), teamMember.getEmail(), teamMember.getIds().iterator().next().getUsername()) + "', please check your values or try again."))));
            }
            catch(ResponseStatusException e){
                throw e;
            }
            catch(IllegalArgumentException e){
                log.warn("Incorrect team member received... ", e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
            catch(Exception e){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong, please try again later or contact support.");
            }
        });
   }

   @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
   public DeferredResult<ResponseEntity<DBTeamMember>> getTeamMember(@SessionAttribute("company") String company, @PathVariable("id") UUID teamMemberId){
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
            teamMembersService.get(company, teamMemberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team member with id '" + teamMemberId.toString() + "' not found"))));
   }

   @DeleteMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
   public DeferredResult<ResponseEntity<String>> deleteTeamMembers(@SessionAttribute("company") String company, @RequestBody() Set<UUID> teamMemberIds){
        return SpringUtils.deferResponse(() -> {
            try {
                teamMembersService.delete(company, teamMemberIds);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok("OK");
        });
   }

   @PostMapping(path = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
   public DeferredResult<ResponseEntity<PaginatedResponse<DBTeamMember>>> listTeamMembers(@SessionAttribute("company") String company, @RequestBody DefaultListRequest request){
        return SpringUtils.deferResponse(() -> {
            var teams = teamMembersService.filter(company, QueryFilter.fromRequestFilters(request.getFilter()), request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), teams.getTotalCount(), teams.getRecords()));
        });
   }
}
