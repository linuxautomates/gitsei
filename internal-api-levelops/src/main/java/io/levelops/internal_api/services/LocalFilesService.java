package io.levelops.internal_api.services;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobField;
import com.google.cloud.storage.Storage.BlobGetOption;
import com.google.cloud.storage.Storage.BlobListOption;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.FileUpload;
import io.levelops.uploads.services.FilesService;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class LocalFilesService implements FilesService {
    private static final String BASE_PATH_FORMAT = "/%s/uploads/%s/%s"; // 1: company. 2: component type. 3: component (identifier).
    private static final String COMPONENT_PATH_FORMAT = BASE_PATH_FORMAT + "/%s"; // 4: item id.
    private static final String SUBCOMPONENT_CONTAINER_PATH_FORMAT = BASE_PATH_FORMAT + "/%s/%s"; // 4:subcomponent type. 5: subcomponent id.
    private static final String SUBCOMPONENT_PATH_FORMAT = SUBCOMPONENT_CONTAINER_PATH_FORMAT + "/%s"; // 6: item.

    private static final String UPLOADING_USER = "uploading-user";
    private Storage storage;
    private String bucketName;

    public LocalFilesService(@Value("${UPLOADS_BUCKET_NAME:levelops-uploads}") final String bucketName,
            final Storage storage) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    @Override
    public UUID uploadForSubComponent(String company, String user, RoleType role, ComponentType component, String componentId,
            String subComponent, String subComponentId, FileUpload fileUpload) throws IOException {
        var id = UUID.randomUUID();
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component.getStorageName(), componentId, subComponent,
                subComponentId, id.toString());
        upload(path, user, role, fileUpload);
        return id;
    }

    @Override
    public UUID uploadForComponent(String company, String user, RoleType role, ComponentType component, String componentId,
            FileUpload fileUpload) throws IOException {
        var id = UUID.randomUUID();
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(COMPONENT_PATH_FORMAT, company, component.getStorageName(), componentId, id.toString());
        upload(path, user, role, fileUpload);
        return id;
    }

    @Override
    public FileUpload downloadForSubComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, String subComponent, String subComponentId, UUID fileId) throws IOException {
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component.getStorageName(), componentId, subComponent, subComponentId, fileId.toString());
        return download(path, user, role);
    }

    @Override
    public FileUpload downloadForComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, UUID fileId) throws IOException {
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(COMPONENT_PATH_FORMAT, company, component.getStorageName(), componentId, fileId.toString());
        return download(path, user, role);
    } 

    @Override
    public boolean updateForSubComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, String subComponent, String subComponentId, UUID fileId, FileUpload fileUpload)
            throws IOException {
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component.getStorageName(), componentId, subComponent,
                subComponentId, fileId);
        return update(path, user, role, fileUpload);
    }

    @Override
    public boolean updateForComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, UUID fileId, FileUpload fileUpload) throws IOException {
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(COMPONENT_PATH_FORMAT, company, component.getStorageName(), componentId, fileId);
        return update(path, user, role, fileUpload);
    }

    @Override
    public boolean deleteForSubComponent(final String company, final String user, final RoleType role, final ComponentType component,
        final String componentId, final String subComponent, final String subComponentId, final UUID fileId) throws IOException {
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component.getStorageName(), componentId, subComponent, subComponentId, fileId.toString());
        return delete(path, user, role);
    }

    @Override
    public boolean deleteForComponent(final String company, final String user, final RoleType role, final ComponentType component,
        final String componentId, final UUID fileId) throws IOException {
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(COMPONENT_PATH_FORMAT, company, component.getStorageName(), componentId, fileId.toString());
        return delete(path, user, role);
    }
    
    @Override
    public boolean deleteEverythingUnderSubComponent(final String company, final String user, final RoleType role, final ComponentType component,
        final String componentId, final String subComponent, final String subComponentId) throws IOException {
        if (component == ComponentType.NONE || component == ComponentType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not supported");
        }
        var path = String.format(SUBCOMPONENT_CONTAINER_PATH_FORMAT, company, component.getStorageName(), componentId);
        return delete(path, user, role);
    }

    @Override
    public boolean deleteEverythingUnderComponent(final String company, final String user, final RoleType role, final ComponentType component,
        final String componentId) throws IOException {
        var path = String.format(BASE_PATH_FORMAT, company, component.getStorageName(), componentId);
        return delete(path, user, role);
    }

    @Override
    public boolean upload(String path, String user, RoleType role, FileUpload fileUpload) throws IOException {
        // upload file to GCS
        Map<String, String> meta = Map.of(
            "name", fileUpload.getUploadName(),
            "filename", fileUpload.getFileName(),
            "content-type", fileUpload.getContentType(),
            UPLOADING_USER, user
        );
        ByteArrayOutputStream byos = new ByteArrayOutputStream();
        IOUtils.copy(fileUpload.getFile(), byos);
        storage.create(
            BlobInfo
                .newBuilder(bucketName, path)
                .setMetadata(meta)
                .build(), 
            byos.toByteArray());
        log.debug("FileUploaded successfully: {}", fileUpload);
        return true;
    }

    @Override
    public FileUpload download(String path, String user, RoleType role) throws IOException {
        Blob blob = storage.get(bucketName, path, BlobGetOption.fields(BlobField.METADATA));
        String outputFile = Files.createTempFile("files_service_", ".download").toAbsolutePath().normalize().toString();
        OutputStream out = new FileOutputStream(outputFile);
        blob.downloadTo(out);
        return FileUpload.builder()
            .fileName(blob.getMetadata().get("filename"))
            .contentType((blob.getMetadata().get("content-type")))
            .file(new FileInputStream(outputFile))
            .build();
    }

    @Override
    public boolean update(String path, String user, RoleType role, FileUpload fileUpload)
            throws IOException {
        var blob = storage.get(bucketName, path, BlobGetOption.fields(BlobField.METADATA));
        if (RoleType.ADMIN != role && !user.equalsIgnoreCase(blob.getMetadata().getOrDefault(UPLOADING_USER, ""))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to update this content.");
        }
        Map<String, String> meta = Map.of(
            "name", fileUpload.getUploadName(),
            "filename", fileUpload.getFileName(),
            "content-type", fileUpload.getContentType(),
            UPLOADING_USER, user
        );
        ByteArrayOutputStream byos = new ByteArrayOutputStream();
        IOUtils.copy(fileUpload.getFile(), byos);
        storage.create(BlobInfo.newBuilder(bucketName, path).setMetadata(meta).build(), byos.toByteArray());
        log.debug("FileUploaded successfully: {}", fileUpload);
        return true;
    }

    @Override
    public boolean delete(String path, String user, RoleType role) throws IOException {
        var blob = storage.get(bucketName, path, BlobGetOption.fields(BlobField.METADATA));
        boolean isFileFound = blob != null;
        if (isFileFound) {
            if (RoleType.ADMIN != role && !user.equalsIgnoreCase(blob.getMetadata().getOrDefault(UPLOADING_USER, ""))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to delete this content.");
            }
            return blob.delete();
        }

        // if the file is not found let's check if it's a folder.
        Page<Blob> page = storage.list(bucketName, BlobListOption.prefix(path), BlobListOption.fields(BlobField.METADATA));
        if (page.getValues().iterator().hasNext() && role != RoleType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authorized to update this content.");
        } 
        else if (!page.getValues().iterator().hasNext()) {
            // maybe return not found exception?? for now return true in an idempotent kind of way.
            return true;
        }
        AtomicBoolean status = new AtomicBoolean(true);
        page.iterateAll().forEach(item -> {
            if (!item.getBlobId().getName().endsWith("/")){
                status.compareAndSet(true, item.delete());
            }
        });
        return status.get();
    }
}