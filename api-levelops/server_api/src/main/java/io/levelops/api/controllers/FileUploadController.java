package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.auth.services.TokenGenService;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.files.exceptions.NotAuthorizedException;
import io.levelops.files.services.FileStorageService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/v1/fileupload")
@Log4j2
@SuppressWarnings("unused")
public class FileUploadController {
    private final TokenGenService tokenGenService;
    private final FileStorageService fileStorageService;

    @Autowired
    public FileUploadController(final TokenGenService tokenGenService,
                                final FileStorageService fileStorageService) {
        this.tokenGenService = tokenGenService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * POST - Creates a fileupload object.
     *
     * @param company
     * @param component      the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId    the id of the instance of the component associted with the file
     * @param subComponent   the name of the subcomponent associated with the file (question, praetorian_report)
     * @param subComponentId the id of the subcomponent associtated with the file
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, path = "/{component}/{component_id}/{sub_component}/{sub_component_id}",
            produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<Map<String, Object>>> uploadNewFileForSubComponent(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String user,
            @PathVariable("component") final String component,
            @PathVariable("component_id") final String componentId,
            @PathVariable("sub_component") final String subComponent,
            @PathVariable("sub_component_id") final String subComponentId,
            @RequestParam(value = "file") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            String id = fileStorageService.uploadNewFileForSubComponent(company, user, component, componentId, subComponent, subComponentId,
                    fileUpload.getName(), fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getInputStream());
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    /**
     * POST - Creates a fileupload object.
     *
     * @param company
     * @param component   the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the instance of the component associted with the file
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, path = "/{component}/{component_id}",
            produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<Map<String, Object>>> uploadNewFileForComponent(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String user,
            @PathVariable("component") final String component,
            @PathVariable("component_id") final String componentId,
            @RequestParam(value = "file") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            String id = fileStorageService.uploadNewFileForComponent(company, user, component, componentId,
                    fileUpload.getName(), fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getInputStream());
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    /**
     * Generic upload file method
     *
     * @param path       GCS path
     * @param id         item id
     * @param fileUpload contents
     * @return
     */
    public DeferredResult<ResponseEntity<Map<String, Object>>> uploadNewFile(final String path, final String id,
                                                                             final String user, final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            fileStorageService.uploadNewFileStream(path, id, user, fileUpload.getName(), fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getInputStream());
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    /**
     * GET - Redirects file download to a url based auth
     * This endpoint only works for NON-API TOKEN queries
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET,
            path = "/{component}/{component_id}/{sub_component}/{sub_component_id}/{file_id}")
    public ResponseEntity<String> subcomponentFileDownloadRedirect(@SessionAttribute("company") final String company,
                                                                   @SessionAttribute("session_user") final String username,
                                                                   @PathVariable("component") final String component,
                                                                   @PathVariable("component_id") final String componentId,
                                                                   @PathVariable("sub_component") final String subComponent,
                                                                   @PathVariable("sub_component_id") final String subComponentId,
                                                                   @PathVariable("file_id") final String id) {
        String token = null;
        try {
            token = tokenGenService.generateShortTermToken(company, username);
        } catch (SQLException throwables) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User not found. This endpoint does not accept apitoken. Use /v1/filedownload instead.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Location",
                "/v1/filedownload/" + component
                        + "/" + componentId
                        + "/" + subComponent
                        + "/" + subComponentId
                        + "/" + id
                        + "?" + ClientConstants.AUTHORIZATION + "=" + token);
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    /**
     * GET - Retrieves a fileupload object for component based uploads.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/{component}/{component_id}/{file_id}")
    public ResponseEntity<String> componentFileDownloadRedirect(@SessionAttribute("company") final String company,
                                                                @SessionAttribute("session_user") final String username,
                                                                @PathVariable("component") final String component,
                                                                @PathVariable("component_id") final String componentId,
                                                                @PathVariable("file_id") final String id) {
        String token = null;
        try {
            token = tokenGenService.generateShortTermToken(company, username);
        } catch (SQLException throwables) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User not found. This endpoint does not accept apitoken. Use /v1/filedownload instead.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Location",
                "/v1/filedownload/" + component
                        + "/" + componentId
                        + "/" + id
                        + "?" + ClientConstants.AUTHORIZATION + "=" + token);
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PUT,
            path = "/{component}/{component_id}/{file_id}",
            produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<Map<String, Object>>> updateFileForComponent(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String user,
            @SessionAttribute("user_type") final RoleType role,
            @PathVariable("component") final String component,
            @PathVariable("component_id") final String componentId,
            @PathVariable("file_id") final String fileId,
            @RequestParam(value = "file") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            try {
                String id = fileStorageService.updateFileForComponent(company, user, role, component, componentId, fileId,
                        fileUpload.getName(), fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getBytes());
                return ResponseEntity.ok(Map.of("id", id));
            } catch (NotAuthorizedException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to update this content.");
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT,
            path = "/{component}/{component_id}/{sub_component}/{sub_component_id}/{file_id}",
            produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<Map<String, Object>>> updateFileForSubComponent(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String user,
            @SessionAttribute("user_type") final RoleType role,
            @PathVariable("component") final String component,
            @PathVariable("component_id") final String componentId,
            @PathVariable("sub_component") final String subComponent,
            @PathVariable("sub_component_id") final String subComponentId,
            @PathVariable("file_id") final String fileId,
            @RequestParam(value = "file") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            try {
                String id = fileStorageService.updateFileForSubComponent(company, user, role, component, componentId, subComponent, subComponentId, fileId,
                        fileUpload.getName(), fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getBytes());
                return ResponseEntity.ok(Map.of("id", id));
            } catch (NotAuthorizedException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to update this content.");
            }
        });
    }

    public DeferredResult<ResponseEntity<Map<String, Object>>> updateFile(final String path, final String fileId, final String user, final RoleType role, final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            try {
                String id = fileStorageService.updateFile(path, fileId, user, role,
                        fileUpload.getName(), fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getBytes());
                return ResponseEntity.ok(Map.of("id", id));
            } catch (NotAuthorizedException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to update this content.");
            }
        });
    }

    /**
     * DELETE - Deletes a fileupload object.
     *
     * @param company        tenant
     * @param component      the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId    the id of the instance of the component associted with the file
     * @param subComponent   the name of the subcomponent associated with the file (question, praetorian_report)
     * @param subComponentId the id of the subcomponent associtated with the file
     * @param id             the id of the file
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.DELETE, path = "/{component}/{component_id}/{sub_component}/{sub_component_id}/{id}")
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteFileForSubComponent(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String user,
            @SessionAttribute("user_type") final RoleType role,
            @PathVariable("component") final String component,
            @PathVariable("component_id") final String componentId,
            @PathVariable("sub_component") final String subComponent,
            @PathVariable("sub_component_id") final String subComponentId,
            @PathVariable(value = "id") final String id) {
        return SpringUtils.deferResponse(() -> {
            try {
                boolean success = fileStorageService.deleteFileForSubComponent(company, user, role, component, componentId, subComponent, subComponentId, id);
                return ResponseEntity.ok(Map.of("deleted", success));
            } catch (NotAuthorizedException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to update this content.");
            }
        });
    }

    /**
     * DELETE - Deletes a fileupload object.
     *
     * @param company
     * @param component   the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the instance of the component associted with the file
     * @param id          the id of the file
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.DELETE, path = "/{component}/{component_id}/{id}")
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteFileForComponent(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String user,
            @SessionAttribute("user_type") final RoleType role,
            @PathVariable("component") final String component,
            @PathVariable("component_id") final String componentId,
            @PathVariable(value = "id") final String id) {
        return SpringUtils.deferResponse(() -> {
            try {
                boolean success = fileStorageService.deleteFileForComponent(company, user, role, component, componentId, id);
                return ResponseEntity.ok(Map.of("deleted", success));
            } catch (NotAuthorizedException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to update this content.");
            }
        });
    }

    /**
     * Generic upload file method
     *
     * @param path GCS path
     * @return
     */
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteFile(final String path, final String user, final RoleType role) {
        return SpringUtils.deferResponse(() -> {
            try {
                boolean success = fileStorageService.deleteFile(path, user, role);
                return ResponseEntity.ok(Map.of("deleted", success));
            } catch (NotAuthorizedException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to update this content.");
            }
        });
    }
}