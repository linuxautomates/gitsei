package io.levelops.commons.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class FileUploadTest {

    @Test
    public void test() throws IOException {
        var uploadName = "My upload";
        var fileName = "my File";
        var contentType = "applicaton/pdf";
        var file = Files.createTempFile("file_upload", ".test");
        var data = Map.of(
            "upload_name", uploadName,
            "file_name", fileName,
            "content_type", contentType,
            "file", file.toFile()
            );
        var upload = DefaultObjectMapper.get().convertValue(data, FileUpload.class);

        Assertions.assertThat(upload.getFileName()).isEqualTo(fileName);
        Assertions.assertThat(upload.getUploadName()).isEqualTo(uploadName);
        Assertions.assertThat(upload.getContentType()).isEqualTo(contentType);
        Assertions.assertThat(upload.getFile()).isNotNull();

        data = Map.of(
            "upload_name", uploadName,
            "file_name", fileName,
            "content_type", contentType
            );
        upload = DefaultObjectMapper.get().convertValue(data, FileUpload.class);

        Assertions.assertThat(upload.getFileName()).isEqualTo(fileName);
        Assertions.assertThat(upload.getUploadName()).isEqualTo(uploadName);
        Assertions.assertThat(upload.getContentType()).isEqualTo(contentType);
        Assertions.assertThat(upload.getFile()).isNull();
    }
}