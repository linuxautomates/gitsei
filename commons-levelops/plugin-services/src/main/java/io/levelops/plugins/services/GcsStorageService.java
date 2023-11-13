package io.levelops.plugins.services;

import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Log4j2
public abstract class GcsStorageService {
    private static final Integer TEN_MBS = 10 * 1024 * 1024;
    public void download(final Storage storage, final String bucketName, final String gcsPath, final File destinationFile) throws IOException {
        log.debug("Starting Download content from {}:{}", bucketName, gcsPath);
        try (FileOutputStream o = new FileOutputStream(destinationFile);
             ReadChannel r = storage.reader(bucketName, gcsPath)) {
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
        log.debug("Completed Download content from {}:{}", bucketName, gcsPath);
    }

    public void upload(final Storage storage, final String bucketName, final String gcsPath, final String contentType, final InputStream is) throws IOException {
        log.info("Uploading '{}' content to {}:{}", contentType, bucketName, gcsPath);
        BlobId blobId = BlobId.of(bucketName, gcsPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        try (WriteChannel w = storage.writer(blobInfo)) {
            byte[] bytes = new byte[TEN_MBS];
            int bytesRead;
            while ((bytesRead = is.read(bytes)) >= 0) {
                w.write(ByteBuffer.wrap(bytes, 0, bytesRead));
            }
        }
    }

    public Blob uploadBytes(final Storage storage, final String bucketName, final String gcsPath, final String contentType, final byte[] content) {
        BlobId blobId = BlobId.of(bucketName, gcsPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        log.info("Uploading '{}' content to {}:{}", contentType, bucketName, gcsPath);
        return storage.create(blobInfo, content);
    }
}
