package io.levelops.aggregations_shared.utils;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class CompressionUtils {
    public static byte[] compress(String str) throws IOException {
        Deflater compressor = new Deflater();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(str.length());
        ByteArrayInputStream inputStream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream, compressor);
        IOUtils.copy(inputStream, deflaterOutputStream);
        deflaterOutputStream.finish();
        deflaterOutputStream.close();
        return outputStream.toByteArray();
    }

    public static String decompress(byte[] compressedData) throws IOException {
        if (compressedData == null) {
            return null;
        }
        Inflater decompresser = new Inflater();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InflaterInputStream inflaterInputStream = new InflaterInputStream(inputStream, decompresser);
        IOUtils.copy(inflaterInputStream, outputStream);
        inflaterInputStream.close();
        return outputStream.toString();
    }
}
