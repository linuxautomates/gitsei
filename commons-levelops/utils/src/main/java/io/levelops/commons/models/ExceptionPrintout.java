package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class ExceptionPrintout {

    @JsonProperty("message")
    private String message;
    @JsonProperty("stacktrace")
    private List<Map.Entry<String, List<String>>> stacktrace;

    public static ExceptionPrintout fromThrowable(Throwable ex) {
        if (ex == null) {
            return null;
        }
        return ExceptionPrintout.builder()
                .message(ex.getMessage())
                .stacktrace(formatStackTrace(ex))
                .build();
    }

    private static List<Map.Entry<String, List<String>>> formatStackTrace(Throwable ex) {
        List<Map.Entry<String, List<String>>> stackTraceChain = Lists.newArrayList();
        List<String> currentStackTrace = Lists.newArrayList();
        for (String frame : ExceptionUtils.getStackFrames(ex)) {
            if (frame.startsWith("\t")) {
                currentStackTrace.add(frame.trim());
            } else {
                currentStackTrace = Lists.newArrayList();
                stackTraceChain.add(Maps.immutableEntry(frame, currentStackTrace));
            }
        }
        return stackTraceChain;
    }


}
