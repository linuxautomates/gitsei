package io.levelops.commons.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipService {
    private static final Logger LOGGER = Logger.getLogger(ZipService.class.getName());
    public void zipDirectory(File sourceDirectory, File outputZipFile) throws IOException {
        LOGGER.info("zipDirectory sourceDirectory = " + sourceDirectory.getAbsolutePath() + " outputZipFile = " + outputZipFile.getAbsolutePath());
        final Queue<File> queue = new LinkedList<>();
        queue.offer(sourceDirectory);

        try (FileOutputStream fos = new FileOutputStream(outputZipFile);
             ZipOutputStream zos = new ZipOutputStream(fos) ) {

            while (!queue.isEmpty()) {
                File currentDir = queue.poll();
                if (currentDir == null) {
                    continue;
                }
                File[] children = currentDir.listFiles();
                if (children == null) {
                    continue;
                }
                for (File currentChild : children) {
                    if (currentChild.isDirectory()) {
                        queue.offer(currentChild);
                    } else {
                        LOGGER.fine("File Added : " + currentChild.toString());
                        ZipEntry ze = new ZipEntry(currentChild.getPath().substring(sourceDirectory.getAbsolutePath().length() +1));
                        zos.putNextEntry(ze);
                        try (FileInputStream in = new FileInputStream(currentChild)){
                            int len;
                            byte[] buffer = new byte[1024];
                            while ((len = in .read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }
                        }
                    }
                }
            }

            zos.closeEntry();
        }
    }

    public void unZip(File sourceZipFile, File unzipDestinationDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceZipFile));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(unzipDestinationDir, zipEntry);
            try (FileOutputStream fos = new FileOutputStream(newFile)){
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        if(!destFile.getParentFile().exists()){
            if(!destFile.getParentFile().mkdirs()){
                throw new IOException("Could not create dir: " + destFile.getParentFile().getAbsolutePath());
            }
        }
        if(!destFile.exists()){
            try {
                if(!destFile.createNewFile()){
                    throw new IOException("Could not create file: " + destFile.getAbsolutePath());
                }
            }
            catch (IOException e) {
                LOGGER.severe("While processing: " + destFilePath);
                throw new IOException("While processing: destDir=" + destinationDir + ", file=" + destFilePath, e);
            }
        }
        return destFile;
    }
}
