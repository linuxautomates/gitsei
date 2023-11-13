package io.levelops.commons.client;

import lombok.extern.log4j.Log4j2;

import java.io.Closeable;

@Log4j2
public class CloseableUtils {
    public static void closeQuietly(Closeable closeable){
        if(closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            log.debug("Exception trying to close.", e);
        }
    }
}
