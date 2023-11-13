package io.levelops.aggregations.utils;

import io.levelops.aggregations.models.messages.AppAggMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import javax.annotation.Nullable;

public class LoggingUtils {

    public static void clearThreadLocalContext() {
        MDC.clear();
    }

    public static void setupThreadLocalContext(@Nullable String messageId,
                                               @Nullable String tenantId,
                                               @Nullable String integrationType,
                                               @Nullable String integrationId) {
        MDC.put("message_id", StringUtils.right(StringUtils.defaultString(messageId), 7));
        MDC.put("tenant_id", StringUtils.defaultString(tenantId));
        if (StringUtils.isNotEmpty(integrationId)) {
            MDC.put("integration_id", integrationId);
        }
        if (StringUtils.isNotEmpty(integrationType)) {
            MDC.put("integration_type", integrationType);
        }
    }

    public static void setupThreadLocalContext(AppAggMessage appAggMessage) {
        setupThreadLocalContext(appAggMessage.getMessageId(), appAggMessage.getCustomer(), appAggMessage.getIntegrationType(), appAggMessage.getIntegrationId());
    }

}
