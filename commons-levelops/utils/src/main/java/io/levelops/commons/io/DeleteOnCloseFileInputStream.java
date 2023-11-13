package io.levelops.commons.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DeleteOnCloseFileInputStream extends FileInputStream {
    private File tempFile;

    /**
     * Creates a FileInputStream that will delete the file once the input stream is closed
     * @param filePath the path to the file (temp) that will be deleted when the stream closes.
     */
    public DeleteOnCloseFileInputStream(String filePath) throws FileNotFoundException {
        super(filePath);
        this.tempFile = new File(filePath);
    }

    /**
     * Creates a FileInputStream that will delete the file once the input stream is closed
     * @param file the file object (temp) that will be deleted when the stream closes.
     */
    public DeleteOnCloseFileInputStream(File tempFile) throws FileNotFoundException {
        super(tempFile);
        this.tempFile = tempFile;
    }

    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (tempFile != null) {
                tempFile.delete();
                tempFile = null;
            }
        }
    }
}