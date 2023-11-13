package io.levelops.etl.utils;

import io.levelops.aggregations_shared.models.JobContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import javax.annotation.Nullable;

// Similar version exists for old aggs, potentially merge and move to commons
public class LoggingUtils {

    public static void clearThreadLocalContext() {
        MDC.clear();
    }

    public static void setupThreadLocalContext(@Nullable String jobInstanceId,
                                               @Nullable String tenantId,
                                               @Nullable String integrationType,
                                               @Nullable String integrationId,
                                               @Nullable String etlProcessorName) {
        MDC.put("message_id", StringUtils.right(StringUtils.defaultString(jobInstanceId), 7));
        MDC.put("tenant_id", StringUtils.defaultString(tenantId));
        MDC.put("etl_processor_name", StringUtils.defaultString(etlProcessorName));
        if (StringUtils.isNotEmpty(integrationId)) {
            MDC.put("integration_id", integrationId);
        }
        if (StringUtils.isNotEmpty(integrationType)) {
            MDC.put("integration_type", integrationType);
        }
    }

    public static void setupThreadLocalContext(JobContext ctx) {
        setupThreadLocalContext(
                ctx.getJobInstanceId().toString(),
                ctx.getTenantId(),
                ctx.getIntegrationType(),
                ctx.getIntegrationId(),
                ctx.getEtlProcessorName()
        );
    }
}
