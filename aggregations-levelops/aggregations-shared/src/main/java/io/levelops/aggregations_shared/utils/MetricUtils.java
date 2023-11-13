package io.levelops.aggregations_shared.utils;

import io.levelops.aggregations_shared.models.JobContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

public class MetricUtils {
    public static Counter getTenantCounter(MeterRegistry meterRegistry, String name, String tenantId, String integrationId, String integrationType, String etlProcessorName) {
        return meterRegistry.counter(name, "tenant_id", tenantId, "integration_id", integrationId, "integration_type", integrationType, "etl_processor_name", etlProcessorName);
    }

    public static Counter getTenantCounter(MeterRegistry meterRegistry, String name, JobContext context) {
        return getTenantCounter(meterRegistry, name, context.getTenantId(), context.getIntegrationId(), context.getIntegrationType(), context.getEtlProcessorName());
    }

    public static <T extends Number> T getTenantGauge(MeterRegistry meterRegistry, String name, String tenantId, String integrationId, String etlProcessorName, T num) {
        return meterRegistry.gauge(name, Tags.of("tenant_id", tenantId, "integration_id", integrationId, "etl_processor_name", etlProcessorName), num);
    }

    public static <T extends Number> T getTenantGauge(MeterRegistry meterRegistry, String name, JobContext context, T num) {
        return getTenantGauge(meterRegistry, name, context.getTenantId(), context.getIntegrationId(), context.getEtlProcessorName(), num);
    }
}
