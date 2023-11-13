package io.levelops.files.services;

import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.files.exceptions.NotAuthorizedException;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.Validate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Value
public class FileStorageService {
    private static final Integer TEN_MBS = 10 * 1024 * 1024;
    private static final String BASE_PATH_FORMAT = "/%s/uploads/%s/%s"; // 1: company. 2: component name. 3: component.
    private static final String SUBCOMPONENT_PATH_FORMAT = BASE_PATH_FORMAT + "/%s/%s/%s"; // 1:subcomponent name. 2: subcomponent id. 3: item.
    private static final String COMPONENT_PATH_FORMAT = BASE_PATH_FORMAT + "/%s"; // 1: item.
    private static final String UPLOADING_USER = "uploading-user";
    
    private final Storage storage;
    private final String bucketName;

    public FileStorageService(Storage storage, String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    @Deprecated
    public String uploadNewFileForComponent(final String company, final String user,
                                            final String component, final String componentId,
                                            final String name, final String fileName, final String contentType, final byte[] fileData) {
        var id = UUID.randomUUID().toString();
        var path = String.format(COMPONENT_PATH_FORMAT, company, component, componentId, id);
        return uploadNewFile(path, id, user, name, fileName, contentType, fileData);
    }
    @Deprecated
    public String uploadNewFileForSubComponent(final String company, final String user,
                                               final String component, final String componentId,
                                               final String subComponent, final String subComponentId,
                                               final String name, final String fileName, final String contentType, final byte[] fileData) {
        var id = UUID.randomUUID().toString();
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component, componentId, subComponent, subComponentId, id);
        return uploadNewFile(path, id, user, name, fileName, contentType, fileData);
    }
    @Deprecated
    public String uploadNewFile(final String path, final String id, final String user, 
                                final String name, final String fileName, final String contentType, final byte[] fileData) {
        // upload file to GCS
        BlobInfo blobInfo = generateBlobInfo(name, fileName, contentType, user, path);
        storage.create(blobInfo, fileData);
        log.info("FileUploaded successfully: name {}, fileName {}", name,fileName);
        return id;
    }

    private BlobInfo generateBlobInfo(final String name, final String fileName, final String contentType, final String user, final String path) {
        Map<String, String> meta = new HashMap<>();
        meta.put("name", name);
        meta.put("filename", fileName);
        meta.put("content-type", contentType);
        meta.put(UPLOADING_USER, user);
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, path).setMetadata(meta).build();
        return blobInfo;
    }

    public String uploadNewFileForComponent(final String company, final String user,
                                            final String component, final String componentId,
                                            final String name, final String fileName, final String contentType, final InputStream is) throws IOException {
        var id = UUID.randomUUID().toString();
        var path = String.format(COMPONENT_PATH_FORMAT, company, component, componentId, id);
        return uploadNewFileStream(path, id, user, name, fileName, contentType, is);
    }

    public String uploadNewFileForSubComponent(final String company, final String user,
                                               final String component, final String componentId,
                                               final String subComponent, final String subComponentId,
                                               final String name, final String fileName, final String contentType, final InputStream is) throws IOException {
        var id = UUID.randomUUID().toString();
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component, componentId, subComponent, subComponentId, id);
        return uploadNewFileStream(path, id, user, name, fileName, contentType, is);
    }

    public String uploadNewFileStream(final String path, final String id, final String user,
                                final String name, final String fileName, final String contentType, final InputStream is) throws IOException {
        // upload file to GCS
        BlobInfo blobInfo = generateBlobInfo(name, fileName, contentType, user, path);
        try (WriteChannel w = storage.writer(blobInfo)) {
            byte[] bytes = new byte[TEN_MBS];
            int bytesRead;
            while ((bytesRead = is.read(bytes)) >= 0) {
                w.write(ByteBuffer.wrap(bytes, 0, bytesRead));
            }
        }
        log.info("FileUploaded successfully: name {}, fileName {}", name,fileName);
        return id;
    }


    public String updateFileForComponent( final String company, final String user, final RoleType role,
            final String component, final String componentId, final String fileId,
            final String name, final String fileName, final String contentType, final byte[] fileData) throws NotAuthorizedException {
        var path = String.format(COMPONENT_PATH_FORMAT, company, component, componentId, fileId);
        return updateFile(path, fileId, user, role, name, fileName, contentType, fileData);
    }

    public String updateFileForSubComponent( final String company, final String user, final RoleType role,
            final String component, final String componentId, final String subComponent, final String subComponentId, final String fileId,
            final String name, final String fileName, final String contentType, final byte[] fileData) throws NotAuthorizedException {
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component, componentId, subComponent, subComponentId, fileId);
        return updateFile(path, fileId, user, role, name, fileName, contentType, fileData);
    }

    public String updateFile(final String path, final String fileId, final String user, final RoleType role,
                             final String name, final String fileName, final String contentType, final byte[] fileData) throws NotAuthorizedException {
        var blob = storage.get(bucketName, path, Storage.BlobGetOption.fields(Storage.BlobField.METADATA));
        if (RoleType.ADMIN != role && !user.equalsIgnoreCase(blob.getMetadata().getOrDefault(UPLOADING_USER, ""))) {
            throw new NotAuthorizedException("User not authorized to update this content.");
        }
        Map<String, String> meta = new HashMap<>();
        meta.put("name", name);
        meta.put("filename", fileName);
        meta.put("content-type", contentType);
        meta.put(UPLOADING_USER, user);
        storage.create(BlobInfo.newBuilder(bucketName, path).setMetadata(meta).build(), fileData);
        log.info("FileUploaded successfully: name {}, fileName {}", name,fileName);
        return fileId;
    }

    public boolean deleteFileForSubComponent( final String company, final String user, final RoleType role,
            final String component, final String componentId, final String subComponent, final String subComponentId, final String fileId) throws NotAuthorizedException {
        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component, componentId, subComponent, subComponentId, fileId);
        return deleteFile(path, user, role);
    }

    public boolean deleteFileForComponent( final String company, final String user, final RoleType role,
            final String component, final String componentId, final String fileId) throws NotAuthorizedException {
        var path = String.format(COMPONENT_PATH_FORMAT, company, component, componentId, fileId);
        return deleteFile(path, user, role);
    }

    public boolean deleteFile(final String path, final String user, final RoleType role) throws NotAuthorizedException {
        var blob = storage.get(bucketName, path, Storage.BlobGetOption.fields(Storage.BlobField.METADATA));
        if (RoleType.ADMIN != role && !user.equalsIgnoreCase(blob.getMetadata().getOrDefault(UPLOADING_USER, ""))) {
            throw new NotAuthorizedException("User not authorized to delete this content.");
        }
        return storage.delete(bucketName, path);
    }

    public byte[] downloadFileForComponent(final String company,
                                            final String component, final String componentId,
                                            final String uploadId) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(component, "component cannot be null or empty.");
        Validate.notBlank(componentId, "componentId cannot be null or empty.");
        Validate.notBlank(uploadId, "uploadId cannot be null or empty.");

        var path = String.format(COMPONENT_PATH_FORMAT, company, component, componentId, uploadId);
        log.debug("Download content form {}:{}", bucketName, path);
        return storage.readAllBytes(bucketName, path);
    }

    public byte[] downloadFileForSubComponent(final String company,
                                            final String component, final String componentId,
                                            final String subComponent, final String subComponentId,
                                            final String uploadId) throws IOException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(component, "component cannot be null or empty.");
        Validate.notBlank(componentId, "componentId cannot be null or empty.");
        Validate.notBlank(subComponent, "subComponent cannot be null or empty.");
        Validate.notBlank(subComponentId, "subComponentId cannot be null or empty.");
        Validate.notBlank(uploadId, "uploadId cannot be null or empty.");

        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component, componentId, subComponent, subComponentId, uploadId);
        log.debug("Downloading content form {}:{}", bucketName, path);
        try (ByteArrayOutputStream byos = new ByteArrayOutputStream()) {
            var blob = storage.get(bucketName, path, Storage.BlobGetOption.fields(Storage.BlobField.METADATA));
            blob.downloadTo(byos);
            return byos.toByteArray();
        }
    }

    public void downloadFileForComponent(final String company,
                                           final String component, final String componentId,
                                           final String uploadId, final File destinationFile) throws IOException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(component, "component cannot be null or empty.");
        Validate.notBlank(componentId, "componentId cannot be null or empty.");
        Validate.notBlank(uploadId, "uploadId cannot be null or empty.");

        var path = String.format(COMPONENT_PATH_FORMAT, company, component, componentId, uploadId);
        log.debug("Download content form {}:{}", bucketName, path);
        try (FileOutputStream o = new FileOutputStream(destinationFile);
             ReadChannel r = storage.reader(bucketName, path)) {
            ByteBuffer bytes = ByteBuffer.allocate(TEN_MBS);
            while (r.read(bytes) > 0) {
                // Flips this buffer.  The limit is set to the current position and then
                // the position is set to zero.  If the mark is defined then it is discarded.
                bytes.flip();
                //Write bytes to file output stream
                o.write(bytes.array());
                //Clear byte buffer
                bytes.clear();
            }
        }
    }

    public void downloadFileForSubComponent(final String company,
                                           final String component, final String componentId,
                                           final String subComponent, final String subComponentId,
                                           final String uploadId, final File destinationFile) throws IOException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(component, "component cannot be null or empty.");
        Validate.notBlank(componentId, "componentId cannot be null or empty.");
        Validate.notBlank(subComponent, "subComponent cannot be null or empty.");
        Validate.notBlank(subComponentId, "subComponentId cannot be null or empty.");
        Validate.notBlank(uploadId, "uploadId cannot be null or empty.");

        var path = String.format(SUBCOMPONENT_PATH_FORMAT, company, component, componentId, subComponent, subComponentId, uploadId);
        log.debug("Download content form {}:{}", bucketName, path);
        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
            var blob = storage.get(bucketName, path, Storage.BlobGetOption.fields(Storage.BlobField.METADATA));
            blob.downloadTo(fos);
        }
    }

}
