package io.levelops.api.controllers;

import com.google.cloud.storage.Storage;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/v1/filedownload")
@Log4j2
@SuppressWarnings("unused")
public class FileDownloadController {
    private static final String BASE_PATH_FORMAT = "/%s/uploads/%s/%s"; // 1: company. 2: component name. 3: component.
    private static final String SUBCOMPONENT_PATH_FORMAT = BASE_PATH_FORMAT + "/%s/%s/%s"; // 1:subcomponent name. 2: subcomponent id. 3: item.
    private static final String COMPONENT_PATH_FORMAT = BASE_PATH_FORMAT + "/%s"; // 1: item.

    private final Storage storage;
    private final String bucketName;

    @Autowired
    public FileDownloadController(final Storage storage,
                                  @Value("${UPLOADS_BUCKET_NAME:levelops-uploads}") final String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'LIMITED_USER' , 'ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/{component}/{component_id}/{sub_component}/{sub_component_id}/{file_id}",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public DeferredResult<ResponseEntity<byte[]>> fileDownloadForSubComponent(@SessionAttribute("company") final String company,
                                                                              @PathVariable("component") final String component,
                                                                              @PathVariable("component_id") final String componentId,
                                                                              @PathVariable("sub_component") final String subComponent,
                                                                              @PathVariable("sub_component_id") final String subComponentId,
                                                                              @PathVariable("file_id") final String id) {
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component, componentId, subComponent, subComponentId, id);
        return downloadFile(path);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'LIMITED_USER', 'ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/{component}/{component_id}/{file_id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public DeferredResult<ResponseEntity<byte[]>> fileDownloadForComponent(@SessionAttribute("company") final String company,
                                                                           @PathVariable("component") final String component,
                                                                           @PathVariable("component_id") final String componentId,
                                                                           @PathVariable("file_id") final String id) {
        var path = String.format(COMPONENT_PATH_FORMAT, company, component, componentId, id);
        return downloadFile(path);
    }

    private DeferredResult<ResponseEntity<byte[]>> downloadFile(final String path) {
        return SpringUtils.deferResponse(() -> {
            try (ByteArrayOutputStream byos = new ByteArrayOutputStream()) {
                var blob = storage.get(bucketName, path, Storage.BlobGetOption.fields(Storage.BlobField.METADATA));
                blob.downloadTo(byos);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentDisposition(ContentDisposition.builder("attachment")
                        .filename(blob.getMetadata().get("filename")).build());
                return new ResponseEntity<>(byos.toByteArray(), headers, HttpStatus.OK);
            }
        });
    }
}
