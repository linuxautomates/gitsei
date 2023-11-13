package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.FileUpload;
import io.levelops.uploads.services.FilesService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/files")
@Log4j2
public class FilesController {

    private final FilesService filesClient;

    @Autowired
    public FilesController(final FilesService filesClient) {
        this.filesClient = filesClient;
    }

    /**
     * POST - Creates a fileupload object.
     *
     * @param company tenant
     * @param component the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the instance of the component associted with the file
     * @param subComponent the name of the subcomponent associated with the file (question, praetorian_report)
     * @param subComponentId the id of the subcomponent associtated with the file
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, path = "/{company}/{component}/{component_id}/{sub_component}/{sub_component_id}",
            produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<UUID>> uploadNewFileForSubComponent(
                                                        @PathVariable("company") final String company, 
                                                        @PathVariable("component") final String component,
                                                        @PathVariable("component_id") final String componentId,
                                                        @PathVariable("sub_component") final String subComponent,
                                                        @PathVariable("sub_component_id") final String subComponentId,
                                                        @RequestParam("user") final String user,
                                                        @RequestParam("role") final RoleType role,
                                                        @RequestParam("upload_name") final String uploadName,
                                                        @RequestParam("file_name") final String fileName,
                                                        @RequestParam("content_type") final String contentType,
                                                        @RequestParam("file") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            UUID id = filesClient.uploadForSubComponent(company, user, role, ComponentType.fromString(component), componentId, subComponent, subComponentId, 
                FileUpload.builder()
                    .contentType(contentType)
                    .fileName(fileName)
                    .uploadName(uploadName)
                    .file(fileUpload.getInputStream())
                    .build());
            return ResponseEntity.ok(id);
        });
    }

    /**
     * POST - Creates a fileupload object.
     *
     * @param company tenant
     * @param component the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the instance of the component associted with the file
     * @return
     */
    @PostMapping(path = "/{company}/{component}/{component_id}", produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<UUID>> uploadNewFileForComponent(
                                                    @PathVariable("company") final String company,
                                                    @PathVariable("component") final String component,
                                                    @PathVariable("component_id") final String componentId,
                                                    @RequestParam("user") final String user,
                                                    @RequestParam("role") final RoleType role,
                                                    @RequestParam("upload_name") final String uploadName,
                                                    @RequestParam("file_name") final String fileName,
                                                    @RequestParam("content_type") final String contentType,
                                                    @RequestParam("file") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            UUID id = filesClient.uploadForComponent(company, user, role, ComponentType.fromString(component), componentId, 
                FileUpload.builder()
                    .contentType(contentType)
                    .fileName(fileName)
                    .uploadName(uploadName)
                    .file(fileUpload.getInputStream())
                    .build());
            return ResponseEntity.ok(id);
        });
    }

    /**
     * GET - Retrieves a fileupload object for subcomponent based uploads.
     *
     * @param company tenant
     * @param id id of file
     * @return
     */
    @GetMapping(path = "/{company}/{component}/{component_id}/{sub_component}/{sub_component_id}/{file_id}", 
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public DeferredResult<ResponseEntity<byte[]>> fileDownloadForSubComponent(@PathVariable("company") final String company,
                                                               @PathVariable("component") final String component,
                                                               @PathVariable("component_id") final String componentId,
                                                               @PathVariable("sub_component") final String subComponent,
                                                               @PathVariable("sub_component_id") final String subComponentId,
                                                               @PathVariable("file_id") final UUID id,
                                                               @RequestParam("user") final String user,
                                                               @RequestParam("role") final RoleType role) {
        return SpringUtils.deferResponse(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(
                filesClient.downloadForSubComponent(
                        company,
                        user,
                        role,
                        ComponentType.fromString(component),
                        componentId,
                        subComponent,
                        subComponentId,
                        id)
                    .getFile(),
                baos);
            return ResponseEntity.ok(baos.toByteArray());
        });
    }

    /**
     * GET - Retrieves a fileupload object for component based uploads.
     *
     * @param company tenant
     * @param id levelops id
     * @return
     */
    @GetMapping(path = "/{company}/{component}/{component_id}/{file_id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public DeferredResult<ResponseEntity<byte[]>> fileDownloadForComponent(@PathVariable("company") final String company,
                                                               @PathVariable("component") final String component,
                                                               @PathVariable("component_id") final String componentId,
                                                               @PathVariable("file_id") final UUID id,
                                                               @RequestParam("user") final String user,
                                                               @RequestParam("role") final RoleType role) {
        return SpringUtils.deferResponse(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(
                filesClient.downloadForComponent(
                        company,
                        user,
                        role,
                        ComponentType.fromString(component),
                        componentId,
                        id)
                    .getFile(),
                baos);
            return ResponseEntity.ok(baos.toByteArray());
        });
    }
    
    /**
     * Update File associated to the subcomponent.
     * 
     * @param company tenant
     * @param component component associated to the file.
     * @param componentId id of the component associated to the file.
     * @param subComponent subcomponent associated to the file.
     * @param subComponentId id of the subcomponent associated to the file.
     * @param fileId the id of the uploaded file to be replaced
     * @param user the user updating the file
     * @param role the role of the requestor for the update
     * @param uploadName the levelops field associated with the file
     * @param fileName the file name
     * @param contentType the mime type of the file
     * @param fileUpload the file
     * @return
     */
    @PutMapping(path = "/{company}/{component}/{component_id}/{sub_component}/{sub_component_id}/{file_id}",
        produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<Boolean>> updateFileForSubComponent(
                                                    @PathVariable("company") final String company,
                                                    @PathVariable("component") final String component,
                                                    @PathVariable("component_id") final String componentId,
                                                    @PathVariable("sub_component") final String subComponent,
                                                    @PathVariable("sub_component_id") final String subComponentId,
                                                    @PathVariable("file_id") final UUID fileId,
                                                    @RequestParam("user") final String user,
                                                    @RequestParam("role") final RoleType role,
                                                    @RequestParam("upload_name") final String uploadName,
                                                    @RequestParam("file_name") final String fileName,
                                                    @RequestParam("content_type") final String contentType,
                                                    @RequestParam(value = "file") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            boolean status = filesClient.updateForSubComponent(company, user, role, ComponentType.fromString(component), componentId,
                subComponent,
                subComponentId,
                fileId,
                FileUpload.builder()
                    .contentType(contentType)
                    .fileName(fileName)
                    .uploadName(uploadName)
                    .file(fileUpload.getInputStream())
                    .build());
            return ResponseEntity.ok(status);
        });
    }

    /**
     * Updates a file located at the path provided.
     * 
     * @param company tenant
     * @param component the component type associated with the file
     * @param componentId the id of the component associated wit the file
     * @param user user updating the file
     * @param role the role of this update requestor.
     * @param uploadName identifier of the file 
     * @param fileName the name of the file 
     * @param contentType the mime type of the file
     * @param fileUpload the file
     * @return true if the file is successfully updated
     */
    @PutMapping(path = "/{company}/{component}/{component_id}/{file_id}",
            produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<Boolean>> updateFileForComponent(
                                                    @PathVariable("company") final String company,
                                                    @PathVariable("component") final String component,
                                                    @PathVariable("component_id") final String componentId,
                                                    @PathVariable("file_id") final UUID fileId,
                                                    @RequestParam("user") final String user,
                                                    @RequestParam("role") final RoleType role,
                                                    @RequestParam("upload_name") final String uploadName,
                                                    @RequestParam("file_name") final String fileName,
                                                    @RequestParam("content_type") final String contentType,
                                                    @RequestParam("file") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            boolean status = filesClient.updateForComponent(company, user, role, ComponentType.fromString(component), componentId,
                fileId,
                FileUpload.builder()
                    .contentType(contentType)
                    .fileName(fileName)
                    .uploadName(uploadName)
                    .file(fileUpload.getInputStream())
                    .build());
            return ResponseEntity.ok(status);
        });
    }

    /**
     * Updates a file located at the path provided.
     * 
     * @param path full path to the file to be updated
     * @param user user updating the file
     * @param role the role of this update requestor.
     * @param uploadName identifier of the file 
     * @param fileName the name of the file 
     * @param contentType the mime type of the file
     * @param fileUpload the file
     * @return true if the file is successfully updated
     */
    @PutMapping(produces = "application/json", consumes = "multipart/form-data")
    public DeferredResult<ResponseEntity<Boolean>> updateFile(
                                                    @RequestParam("path") final String path,
                                                    @RequestParam("user") final String user,
                                                    @RequestParam("role") final RoleType role,
                                                    @RequestParam("upload_name") final String uploadName,
                                                    @RequestParam("file_name") final String fileName,
                                                    @RequestParam("content_type") final String contentType,
                                                    @RequestParam("file") final MultipartFile fileUpload) {

        return SpringUtils.deferResponse(() -> {
            filesClient.update(path, user, role, FileUpload.builder()
                .contentType(contentType)
                .fileName(fileName)
                .uploadName(uploadName)
                .file(fileUpload.getInputStream())
                .build());
            
            log.info("FileUploaded successfully: {}", path);
            return ResponseEntity.ok(true);
        });
    }

    /**
     * DELETE - Deletes a fileupload object.
     *
     * @param company tenant
     * @param component the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the instance of the component associted with the file
     * @param subComponent the name of the subcomponent associated with the file (question, praetorian_report)
     * @param subComponentId the id of the subcomponent associtated with the file
     * @param id the id of the file
     * @return
     */
    @DeleteMapping(path = "/{company}/{component}/{component_id}/{sub_component}/{sub_component_id}/{id}")
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteFileForSubComponent(
                                                        @PathVariable("company") final String company,
                                                        @PathVariable("component") final String component,
                                                        @PathVariable("component_id") final String componentId,
                                                        @PathVariable("sub_component") final String subComponent,
                                                        @PathVariable("sub_component_id") final String subComponentId,
                                                        @RequestParam("user") final String user,
                                                        @RequestParam("role") final RoleType role,
                                                        @RequestParam("id") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("deleted", filesClient.deleteForSubComponent(
            company, user, role, ComponentType.fromString(component), componentId, subComponent, subComponentId, id))));
    }

    /**
     * DELETE - Deletes a fileupload object.
     *
     * @param company tenant
     * @param component the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the instance of the component associted with the file
     * @param id the id of the file
     * @return
     */
    @DeleteMapping(path = "/{company}/{component}/{component_id}/{id}")
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteFileForComponent(
                                                        @PathVariable("company") final String company,
                                                        @PathVariable("component") final String component,
                                                        @PathVariable("component_id") final String componentId,
                                                        @RequestParam("user") final String user,
                                                        @RequestParam("role") final RoleType role,
                                                        @RequestParam(value = "id") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("deleted", filesClient.deleteForComponent(
            company, user, role, ComponentType.fromString(component), componentId, id))));
    }

    /**
     * DELETE - Deletes all the contents under the subcomponent specified.
     *
     * @param company tenant
     * @param component the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the instance of the component associted with the file
     * @param subComponent the name of the subcomponent associated with the file (question, praetorian_report)
     * @param subComponentId the id of the subcomponent associtated with the file
     * @return
     */
    @DeleteMapping(path = "/{company}/{component}/{component_id}/{sub_component}/{sub_component_id}")
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteEverythingUnderSubComponent(
                                                        @PathVariable("company") final String company,
                                                        @PathVariable("component") final String component,
                                                        @PathVariable("component_id") final String componentId,
                                                        @PathVariable("sub_component") final String subComponent,
                                                        @PathVariable("sub_component_id") final String subComponentId,
                                                        @RequestParam("user") final String user,
                                                        @RequestParam("role") final RoleType role) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("deleted", filesClient.deleteEverythingUnderSubComponent(
            company, user, role, ComponentType.fromString(component), componentId, subComponent, subComponentId))));
    }

    /**
     * DELETE - Deletes all the contents under the component specified.
     *
     * @param company tenant
     * @param component the name of the component associated with the file (ticket, workitem, plugin_result, etc)
     * @param componentId the id of the componenet to delete including all its contents.
     * @return
     */
    @DeleteMapping(path = "/{company}/{component}/{component_id}")
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteEverythingUnderComponent(
                                                        @PathVariable("company") final String company,
                                                        @PathVariable("component") final String component,
                                                        @PathVariable("component_id") final String componentId,
                                                        @RequestParam("user") final String user,
                                                        @RequestParam("role") final RoleType role) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("deleted", filesClient.deleteEverythingUnderComponent(
            company, user, role, ComponentType.fromString(component), componentId))));
    }

    /**
     * Helper method for spring deletions.
     * 
     * @param path the full path to be deleted, a file or a folder (prefix)
     * @param user the user deleting the location
     * @param role the role of the requestor of the deletion
     * @return
     */
    @DeleteMapping(path = "")
    public DeferredResult<ResponseEntity<Map<String, Object>>> delete(final String path, final String user, final RoleType role) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("deleted", filesClient.delete(path, user, role))));
    }
}