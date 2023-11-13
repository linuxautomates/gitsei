package io.levelops.contributor.service;

import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.contributor.model.SEIContributor;
import io.levelops.contributor.model.SEIIntegrationContributor;
import io.levelops.contributor.model.SEIOrgContributor;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class SEIContributorService {

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public SEIContributorService(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    public SEIContributor list(String company){

        String sql = "select ou.id as ou_id, ou.ref_id, ou.email, ou.full_name, iu.id as iu_id, iu.cloud_id, iu.integration_id, i.application\n" +
                " from "+company+".org_users ou inner join "+company+".org_user_cloud_id_mapping ocm\n" +
                " on ou.id = ocm.org_user_id and ou.versions @> ARRAY(SELECT version FROM "+company+".org_version_counter WHERE type = 'USER' AND active = true)\n" +
                " inner join "+company+".integration_users iu on ocm.integration_user_id = iu.id\n" +
                " inner join "+company+".integrations i on iu.integration_id = i.id \n" +
                " and (i.application in ('github', 'gitlab', 'bitbucket', 'bitbucker_server', 'perforce')\n" +
                " OR ( i.application = 'azure_devops' and metadata->>'subtype' = 'scm'))\n" +
                " group by ou.id, ou.ref_id, ou.email, ou.full_name, iu.id, iu.integration_id, iu.cloud_id, i.application\n" +
                " order by ou.ref_id\n";

        log.info("SEI-Contributor sql "+sql);

        return template.query(sql, Map.of(), new ResultSetExtractor<SEIContributor>() {
            @Override
            public SEIContributor extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<String, SEIOrgContributor> tempMap = Maps.newHashMap();
                int integrationUserCount = 0;
                while(rs.next()){

                    UUID orgUserId =  rs.getObject("ou_id", java.util.UUID.class);
                    String orgUserRefId =  rs.getString("ref_id");
                    String email =  rs.getString("email");
                    String fullName =  rs.getString("full_name");
                    UUID integrationUserId =  rs.getObject("iu_id", java.util.UUID.class);
                    String cloudId =  rs.getString("cloud_id");
                    String integrationId =  rs.getString("integration_id");
                    String application =  rs.getString("application");

                    SEIIntegrationContributor integrationUserDetails = SEIIntegrationContributor.builder()
                            .integrationUserId(integrationUserId)
                            .cloudId(cloudId)
                            .integrationId(Integer.valueOf(integrationId))
                            .integrationType(IntegrationType.fromString(application))
                            .build();

                    SEIOrgContributor orgUserDetails = tempMap.getOrDefault(orgUserRefId, SEIOrgContributor.builder()
                                    .orgUserId(orgUserId)
                                    .orgUserRefId(orgUserRefId)
                                    .email(email)
                                    .fullName(fullName)
                                    .seiIntegrationContributor(new ArrayList<SEIIntegrationContributor>())
                            .build());

                    orgUserDetails.getSeiIntegrationContributor().add(integrationUserDetails);

                    tempMap.put(orgUserRefId, orgUserDetails);
                    integrationUserCount++;
                }
                return SEIContributor.builder()
                        .orgUserCount(tempMap.size())
                        .integrationUserCount(integrationUserCount)
                        .orgUserDetailsList(tempMap.values().stream().collect(Collectors.toList()))
                        .build();
            }
        });
    }
}
