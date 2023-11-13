package io.levelops.etl.jobs.github;

import io.levelops.aggregations_shared.helpers.GithubAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.inventory.ProductMappingService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.github.models.GithubRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Log4j2
@Service
public class GithubPrStage extends BaseIngestionResultProcessingStage<GithubRepository, GithubJobState> {
    private final GithubAggHelperService helper;
    private final ProductMappingService productMappingService;
    private final ControlPlaneService controlPlaneService;


    @Autowired
    public GithubPrStage(GithubAggHelperService helper, ProductMappingService productMappingService, ControlPlaneService controlPlaneService) {
        this.helper = helper;
        this.productMappingService = productMappingService;
        this.controlPlaneService = controlPlaneService;
    }

    @Override
    public String getName() {
        return "Github PR Stage";
    }

    @Override
    public void preStage(JobContext context, GithubJobState jobState) throws SQLException {
        jobState.setProductIds(productMappingService.getProductIds(context.getTenantId(), context.getIntegrationId()));
    }

    @Override
    public void process(JobContext context, GithubJobState jobState, String ingestionJobId, GithubRepository entity) throws SQLException {
        JobDTO ingestionJobDto = null;
        try {
            ingestionJobDto = context.getIngestionJobDto(ingestionJobId, controlPlaneService);
        } catch (IngestionServiceException e) {
            throw new RuntimeException(e);
        }
        helper.processRepositoryPrs(entity, context.getTenantId(), context.getIntegrationId(), ingestionJobDto, jobState.getProductIds());
    }

    @Override
    public void postStage(JobContext context, GithubJobState jobState) {
    }

    @Override
    public String getDataTypeName() {
        return "pull_requests";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }
}
