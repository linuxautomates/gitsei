package io.levelops.commons.tenant_management.services;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static io.levelops.commons.tenant_management.services.TenantConfigDBService.TENANT_CONFIGS_FULL_NAME;
import static io.levelops.commons.tenant_management.services.TenantIndexSnapshotDBService.TENANT_INDEX_SNAPSHOTS_FULL_NAME;
import static io.levelops.commons.tenant_management.services.TenantIndexTypeConfigDBService.TENANT_INDEX_TYPE_CONFIGS_FULL_NAME;

@Log4j2
@Service
public class TenantPriorityManagementService {
    private static final String TENANT_CONFIG_PRIORITY_UPDATE_SQL = "UPDATE " + TENANT_CONFIGS_FULL_NAME +
            " SET priority = (:priority), updated_at = now() ";
    private static final String TENANT_INDEX_CONFIG_PRIORITY_UPDATE_SQL = "UPDATE " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME +
            " SET priority = (:priority), updated_at = now() ";
    private static final String TENANT_INDEX_SNAPSHOT_PRIORITY_UPDATE_SQL = "UPDATE " + TENANT_INDEX_SNAPSHOTS_FULL_NAME +
            " SET priority = (:priority), updated_at = now() ";

    private final PlatformTransactionManager transactionManager;
    private final NamedParameterJdbcTemplate template;

    public TenantPriorityManagementService(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
    }

    public void updateTenantPriority(String tenantId, Integer priority) {
        if (priority == null) {
            return;
        }

        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            updateTenantPriorityInternal(tenantId, priority);
            updateConfigPriorityInternal(null, tenantId, priority);
            updateSnapshotPriorityInternal(null, null, tenantId, priority);
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    private void updateSnapshotPriorityInternal(UUID id, UUID indexConfigId, String tenantId, Integer priority) {

        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("priority", priority);

        if (id != null) {
            criterias.add("id = :id");
            params.put("id", id);
        }

        if (indexConfigId != null) {
            criterias.add("index_type_config_id = :index_type_config_id");
            params.put("index_type_config_id", indexConfigId);
        }

        if (StringUtils.isNotBlank(tenantId)) {
            String tenantConfigIdSelect = "SELECT id FROM " + TENANT_CONFIGS_FULL_NAME + " WHERE tenant_id = :tenant_id ";
            String indexConfigIdSelect = "SELECT id FROM " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " WHERE tenant_config_id IN (" + tenantConfigIdSelect + ")";
            criterias.add("index_type_config_id IN (" + indexConfigIdSelect + ")");
            params.put("tenant_id", tenantId);
        }

        String criteria = "";
        if (CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        String sql = TENANT_INDEX_SNAPSHOT_PRIORITY_UPDATE_SQL + criteria;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        this.template.update(sql, params);
    }

    private void updateTenantPriorityInternal(String tenantId, Integer priority) {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("priority", priority);

        if (StringUtils.isNotBlank(tenantId)) {
            criterias.add("tenant_id = :tenant_id");
            params.put("tenant_id", tenantId);
        }

        String criteria = "";
        if (CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }
        String sql = TENANT_CONFIG_PRIORITY_UPDATE_SQL + criteria;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        this.template.update(sql, params);
    }

    private void updateConfigPriorityInternal(UUID id, String tenantId, Integer priority) {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("priority", priority);

        if (id != null) {
            criterias.add("id = :id");
            params.put("id", id);
        }
        if (StringUtils.isNotBlank(tenantId)) {
            criterias.add("tenant_config_id = (SELECT id FROM " + TENANT_CONFIGS_FULL_NAME + " WHERE tenant_id = :tenant_id )");
            params.put("tenant_id", tenantId);
        }

        String criteria = "";
        if (CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        String sql = TENANT_INDEX_CONFIG_PRIORITY_UPDATE_SQL + criteria;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        this.template.update(sql, params);
    }

    public void updateConfigPriority(UUID configId, Integer priority) {
        if (priority == null) {
            return;
        }
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);

        try {
            updateConfigPriorityInternal(configId, null, priority);
            updateSnapshotPriorityInternal(null, configId, null, priority);
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    public void updateIndexSnapshotPriority(UUID id, Integer priority) {
        if (priority == null) {
            return;
        }
        updateSnapshotPriorityInternal(id, null, null, priority);
    }
}
