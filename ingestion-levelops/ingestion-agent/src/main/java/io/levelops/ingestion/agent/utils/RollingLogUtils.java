package io.levelops.ingestion.agent.utils;

import io.levelops.commons.io.PeekableFilterOutputStream;
import io.levelops.commons.io.PeekablePrintStream;
import io.levelops.commons.io.RollingOutputStream;

public class RollingLogUtils {

    public static RollingOutputStream initRollingLog(int rollingLogSizeInBytes) {
        RollingOutputStream rollingOutputStream = new RollingOutputStream(true, rollingLogSizeInBytes);
        PeekableFilterOutputStream outFos = new PeekableFilterOutputStream(System.out, rollingOutputStream);
        PeekableFilterOutputStream errFos = new PeekableFilterOutputStream(System.err, rollingOutputStream);
        PeekablePrintStream out = new PeekablePrintStream(outFos, true);
        PeekablePrintStream err = new PeekablePrintStream(errFos, true);
        System.setOut(out);
        System.setErr(err);
        return rollingOutputStream;
    }

}
