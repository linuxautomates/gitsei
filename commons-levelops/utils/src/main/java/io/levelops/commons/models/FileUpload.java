package io.levelops.commons.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FileUpload.FileUploadBuilderImpl.class)
public class FileUpload {
    private String uploadName;
    private String fileName;
    private String contentType;
    private InputStream file;

    @JsonPOJOBuilder(withPrefix = "")
    static final class FileUploadBuilderImpl extends FileUploadBuilder {
        public FileUploadBuilderImpl file(final File file) throws FileNotFoundException {
            this.file(new FileInputStream(file));
            return this;
        }
    }
}