package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.mappings.ComponentProductMapping;
import io.levelops.commons.databases.models.database.mappings.ComponentProductMapping.Key;
import io.levelops.commons.databases.utils.SqlInsertQuery;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

@Log4j2
@Service
public class ComponentProductMappingService extends DatabaseService<ComponentProductMapping> {
    private static final String TABLE_NAME = "component_product_mappings";

    private final ObjectMapper mapper;
    protected final NamedParameterJdbcTemplate template;

    @Autowired
    protected ComponentProductMappingService(ObjectMapper mapper, DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    /**
     * Not Supported
     */
    @Override
    public Optional<ComponentProductMapping> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException(
                "Cannot update a single mapping. Use update with the old list of mappings (to delete) and the list of new mappings (insert)");
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(ProductService.class);
    }

    public Optional<ComponentProductMapping> get(String company, ComponentProductMapping.Key mappingKey) throws SQLException {
        DbListResponse<ComponentProductMapping> results = listByCriteria(company, mappingKey, 0, 1);
        if (results != null && results.getCount() > 0){
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<ComponentProductMapping> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return listByCriteria(company, null, pageNumber, pageSize);
    }

    public DbListResponse<ComponentProductMapping> listByCriteria(String company, ComponentProductMapping.Key criteria, Integer pageNumber, Integer pageSize)
            throws SQLException {
        int limit = MoreObjects.firstNonNull(pageSize, 50);
        int skip = MoreObjects.firstNonNull(pageNumber, 0) * limit;
        Map<String, Object> params = new HashMap<>();
        List<String> sqlConditions = new ArrayList<>();
        if (criteria != null){
            if (criteria.getComponentId() != null){
                sqlConditions.add("component_id = :component_id");
                params.put("component_id", criteria.getComponentId());
            }
            if (Strings.isNotBlank(criteria.getComponentType())){
                sqlConditions.add("component_type = :component_type");
                params.put("component_type", criteria.getComponentType());
            }
            if (criteria.getProductId() != null){
                sqlConditions.add("product_id = :product_id");
                params.put("product_id", criteria.getProductId());
            }
        }
        String sql = "SELECT component_type, component_id, array_agg(product_id) as products FROM " + company + "." + TABLE_NAME + 
                (sqlConditions.size() > 0 ? " WHERE " + String.join(" AND ", sqlConditions) : "") +
                " GROUP BY component_type, component_id" +
                " OFFSET :skip " +
                " LIMIT :limit ";
        params.put("skip", skip);
        params.put("limit", limit);

        List<ComponentProductMapping.Key> results = template.query(sql, params, (rs, rowNum) -> {
            return ComponentProductMapping.Key.builder()
                .componentId((UUID)rs.getObject("component_id"))
                .componentType(rs.getString("component_type"))
                .productId(rs.getInt("product_id"))
                .build();
        });
        Map<String, ComponentProductMapping> tmp = new HashMap<>();

        results.stream().forEach(key -> {
            String refKey = key.getComponentType() + "::" + key.getComponentId();
            ComponentProductMapping ref = tmp.get(refKey);
            if(tmp.get(key) == null){
                ref = ComponentProductMapping.builder().componentId(key.getComponentId()).componentType(key.getComponentType()).productIds(new ArrayList<>()).build();
            }
            ref.getProductIds().add(key.getProductId());
        });

        return DbListResponse.of(tmp.values().stream().collect(Collectors.toList()), null);
    }

    /**
     * Single mapping insert (one product).
     * 
     * @param company the company
     * @param mapping the mapping to insert. the productIds must contain only one
     *                element in this method. For multiple product ids use
     *                batchInsert.
     * @return the if of the mapping
     * @throws SQLException
     */
    @Override
    public String insert(String company, ComponentProductMapping mapping) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            return insert(company, mapping, conn);
        }));
    }

    /**
     * Single mapping insert (one product).
     * 
     * @param company the company
     * @param mapping Mappings to insert. The list of productIds can contain only one product id.
     * @param conn    connection to use for the insert. If part of a transaction,
     *                this is usefull to share the transaction across other object.
     * @return the if of the mapping
     * @throws SQLException
     */
    public String insert(String company, ComponentProductMapping mapping, Connection conn) throws SQLException {
        if (mapping.getProductIds().size() > 1) {
            throw new SQLException(
                    "Attemp to insert multiple relations of component to products in a single relation method. If this is the intended use please use batchInsert.");
        }
        batchInsert(company, Collections.singletonList(mapping), conn);
        return "ok";
    }

    /**
     * Multi mapping insert (multiple products).
     * 
     * @param company the company
     * @param mapping Mappings to insert. The productIds object can contain multiple products ids.
     * @return the if of the mapping
     * @throws SQLException
     */
    public void batchInsert(String company, ComponentProductMapping mappings) throws SQLException {
        template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            batchInsert(company, mappings, conn);
            return null;
        }));
    }

    /**
     * Multi mapping insert (multiple products).
     * 
     * @param company the company
     * @param mapping Mappings to insert. The productIds object can contain multiple products ids.
     * @param conn    connection to use for the insert. If part of a transaction,
     *                this is usefull to share the transaction across other object.
     * @return the if of the mapping
     * @throws SQLException
     */
    public void batchInsert(String company, ComponentProductMapping mapping, Connection conn) throws SQLException {
        batchInsert(company, List.of(mapping), conn);
    }

    /**
     * Multi mapping insert (multiple products and multiple mappings).
     * 
     * @param company the company
     * @param mappings List of mappings to insert. The list of productIds on each mapping 
     *                 can contain multiple products ids.
     * @return the if of the mapping
     * @throws SQLException
     */
    public void batchInsert(String company, List<ComponentProductMapping> mappings) throws SQLException {
        template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            batchInsert(company, mappings, conn);
            return null;
        }));
    }

    /**
     * Multi mapping insert (with multiple product ids).
     * 
     * @param company  the company
     * @param mappings List of mappings to insert. The productIds one each mapping
     *                 can contain multiple product ids.
     * @param conn     connection to use for the insert. If part of a transaction,
     *                 this is usefull to share the transaction across other object.
     * @return the if of the mapping
     * @throws SQLException
     */
    public void batchInsert(String company, List<ComponentProductMapping> mappings, Connection conn)
            throws SQLException {
        
        for (ComponentProductMapping mapping : mappings) {
            for (Integer productId : mapping.getProductIds()) {
                SqlInsertQuery insertQuery = SqlInsertQuery.builder(mapper)
                        .schema(company)
                        .table(TABLE_NAME)
                        .field("component_type", mapping.getComponentType())
                        .field("component_id", mapping.getComponentId())
                        .field("product_id", productId)
                        .onConflict("component_type, component_id, product_id", "DO NOTHING")
                        .build();
                PreparedStatement insertStatement = insertQuery.prepareStatement(conn);
                insertStatement.executeUpdate();
            }
        }
    }

    /**
     * Not Supported
     */
    @Override
    public Boolean update(String company, ComponentProductMapping mapping) throws SQLException {
        throw new UnsupportedOperationException(
                "Cannot update a single mapping. Use update with the old list of mappings (to delete) and the list of new mappings (insert)");
    }

    public Boolean update(String company, ComponentProductMapping oldMapping, ComponentProductMapping newMapping) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            return update(company, oldMapping, newMapping, conn);
        }));
    }

    public Boolean update(String company, ComponentProductMapping oldMapping, ComponentProductMapping newMapping, Connection conn) throws SQLException {
        Boolean status = batchDelete(company, extractMappingKeys(oldMapping), conn);
        insert(company, newMapping, conn);
        return status;
    }

    public Boolean batchUpdate(String company, List<ComponentProductMapping> oldMappings, List<ComponentProductMapping> newMappings) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            return batchUpdate(company, oldMappings, newMappings, conn);
        }));
    }

    public Boolean batchUpdate(String company, List<ComponentProductMapping> oldMappings, List<ComponentProductMapping> newMappings, Connection conn) throws SQLException {
        Boolean status = batchDelete(company, extractMappingKeys(oldMappings), conn);
        batchInsert(company, newMappings, conn);
        return status;
    }

    /**
     * Not Supported
     */
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException("Cannot delete component - product mappings by id. Please use delete with component type, component id and/or product id.");
    }

    /**
     * Deletes a single mapping from the ComponentProductMapping table. the key needs to provide component type, component id and product id.
     * 
     * @param company the company
     * @param deleteKey mapping key to delete
     * @return true if all the elements in the deleteKey where deleted. A false value indicates that some records where not found for deletion or where found but couldn't be deletes.
     * @throws SQLException
     */
    public Boolean delete(String company, ComponentProductMapping.Key deleteKey) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            return delete(company, deleteKey, conn);
        }));
    }

    /**
     * Deletes a single mapping from the ComponentProductMapping table. the key needs to provide component type, component id and product id.
     * 
     * @param company the company
     * @param deleteKey mapping key to delete
     * @param conn a connection that can be comming from a transaction
     * @return true if all the elements in the deleteKey where deleted. A false value indicates that some records where not found for deletion or where found but couldn't be deletes.
     * @throws SQLException
     */
    public Boolean delete(String company, ComponentProductMapping.Key deleteKey, Connection conn) throws SQLException {
        if (deleteKey.getComponentId() == null || Strings.isBlank(deleteKey.getComponentType()) || deleteKey.getProductId() == null){
            throw new SQLException("All values in the key are required to delete a single mapping. To delete multiple mappings trigger batchDelete by passing a list of keys.");
        }
        return batchDelete(company, Collections.singletonList(deleteKey), conn);
    }

    /**
     * Deletes a batch of records based on the values of the provided keys.<br/><br />
     * Mappings can be deleted by: <br />
     * <ul>
     *  <li>Matching mapping - needs to provide component type, component id and product id. <li/>
     *  <li>Matching component - needs to provide component type and component id without product id<li=/>
     *  <li>Matching component type - needs to provide component type without product id or component id <li=/>
     *  <li>Matching product - needs to provide product id without componetn type or component id <li/>
     * </ul>
     * @param company the company
     * @param deleteKeys the list of keys to delete
     * @return true if all the elements in the deleteKeys where deleted. A false value indicates that some records where not found for deletion or where found but couldn't be deletes.
     * @throws SQLException
     */
    public Boolean batchDelete(String company, List<ComponentProductMapping.Key> deleteKeys) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            return batchDelete(company, deleteKeys, conn);
        }));
    }

    /**
     * Deletes a batch of records based on the values of the provided keys.<br/><br />
     * Mappings can be deleted by: <br />
     * <ul>
     *  <li>Matching mapping - needs to provide component type, component id and product id. <li/>
     *  <li>Matching component - needs to provide component type and component id without product id<li=/>
     *  <li>Matching component type - needs to provide component type without product id or component id <li=/>
     *  <li>Matching product - needs to provide product id without componetn type or component id <li/>
     * </ul>
     * @param company the company
     * @param deleteKeys the list of keys to delete
     * @param conn a connection that can be comming from a transaction
     * @return true if all the elements in the deleteKeys where deleted. A false value indicates that some records where not found for deletion or where found but couldn't be deletes.
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public Boolean batchDelete(String company, List<ComponentProductMapping.Key> deleteKeys, Connection conn) throws SQLException {
        List<Map<String, Object>> valuesByFullKey = new ArrayList<>();
        List<Map<String, Object>> valuesByProductId = new ArrayList<>();
        List<Map<String, Object>> valuesByComponent = new ArrayList<>();
        List<Map<String, Object>> valuesByComponentType = new ArrayList<>();
        for (ComponentProductMapping.Key deleteKey: deleteKeys){
            if (deleteKey.getComponentId() != null && Strings.isBlank(deleteKey.getComponentType())){
                throw new SQLException("Deleting by component id requires to specify the component type");
            }
            // By full key
            if (deleteKey.getComponentId() != null && Strings.isNotBlank(deleteKey.getComponentType()) && deleteKey.getProductId() != null){
                valuesByFullKey.add(Map.of(
                    "component_id", deleteKey.getComponentId(),
                    "component_type", deleteKey.getComponentType(),
                    "product_id", deleteKey.getProductId()
                    ));
            }
            // By component
            if (deleteKey.getComponentId() != null && Strings.isNotBlank(deleteKey.getComponentType()) && deleteKey.getProductId() == null){
                valuesByComponent.add(Map.of(
                    "component_id", deleteKey.getComponentId(),
                    "component_type", deleteKey.getComponentType()
                    ));
            }
            // By component type
            if (deleteKey.getComponentId() == null && Strings.isNotBlank(deleteKey.getComponentType()) && deleteKey.getProductId() == null){
                valuesByComponentType.add(Map.of(
                    "component_type", deleteKey.getComponentType()
                    ));
            }
            // By product Id
            if (deleteKey.getComponentId() == null && Strings.isBlank(deleteKey.getComponentType()) && deleteKey.getProductId() != null){
                valuesByProductId.add(Map.of(
                    "product_id", deleteKey.getProductId()
                    ));
            }
        }
        String conditionsByFullKey = "component_id = :component_id AND component_type = :component_type AND product_id = :product_id";
        String conditionsByComponent = "component_type = :component_type AND product_id = :product_id";
        String conditionsByComponentType = "component_type = :component_type";
        String conditionsByProductId = "product_id = :product_id";
        String sql = "DELETE " +
                " FROM " + company + "." + TABLE_NAME +
                " WHERE ";
        boolean status = true;
        if (valuesByFullKey.size() > 0){
            int[] results = template.batchUpdate(sql + conditionsByFullKey, (Map<String, Object>[])valuesByFullKey.toArray());
            for (int result:results){
                if(result <= 1){
                    status = false;
                    break;
                }
            }
        }
        if (valuesByComponent.size() > 0){
            int[] results = template.batchUpdate(sql + conditionsByComponent, (Map<String, Object>[])valuesByComponent.toArray());
            for (int result:results){
                if(result <= 1){
                    status = false;
                    break;
                }
            }
        }
        if (valuesByComponentType.size() > 0){
            int[] results = template.batchUpdate(sql + conditionsByComponentType, (Map<String, Object>[])valuesByComponentType.toArray());
            for (int result:results){
                if(result <= 1){
                    status = false;
                    break;
                }
            }
        }
        if (valuesByProductId.size() > 0){
            int[] results = template.batchUpdate(sql + conditionsByProductId, (Map<String, Object>[])valuesByProductId.toArray());
            for (int result:results){
                if(result <= 1){
                    status = false;
                    break;
                }
            }
        }
        return status;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + TABLE_NAME +
                        "(" +
                        "        component_type    VARCHAR(50) NOT NULL," +
                        "        component_id      UUID NOT NULL," +
                        "        product_id        INTEGER NOT NULL REFERENCES " + company + ".products(id) ON DELETE CASCADE," +
                        "        primary key (component_type, component_id, product_id)" +
                        ")",

                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_component_type_product_id_idx       on " + company + "." + TABLE_NAME + " (component_type, product_id)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_product_id_idx    on " + company + "." + TABLE_NAME + " (product_id)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    public List<Key> extractMappingKeys(ComponentProductMapping mapping) {
        if (CollectionUtils.isNotEmpty(mapping.getProductIds())){
            return mapping.getProductIds().stream()
                .map(productId -> ComponentProductMapping.Key.builder()
                    .componentId(mapping.getComponentId())
                    .componentType(mapping.getComponentType())
                    .productId(productId)
                    .build())
                .collect(Collectors.toList());
        }
        return Collections.singletonList(ComponentProductMapping.Key.builder()
            .componentId(mapping.getComponentId())
            .componentType(mapping.getComponentType())
            .build());
        
    }

    public List<Key> extractMappingKeys(List<ComponentProductMapping> mappings) {
        return mappings.stream().flatMap(mapping -> extractMappingKeys(mapping).stream()).collect(Collectors.toList());
    }

}