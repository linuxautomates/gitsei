package io.levelops.integrations.awsdevtools.sources;

import com.amazonaws.services.codebuild.model.AWSCodeBuildException;
import com.amazonaws.services.codebuild.model.BuildBatch;
import com.amazonaws.services.codebuild.model.ListBuildBatchesResult;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClient;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientException;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientFactory;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import io.levelops.integrations.awsdevtools.models.CBBuildBatch;
import io.levelops.integrations.awsdevtools.services.AWSDevToolsEnrichmentService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AWSDevTools's implementation of the {@link DataSource}. This class can be used to fetch build batch data
 * from CodeBuild.
 */
@Log4j2
public class AWSDevToolsBuildBatchDataSource implements DataSource<CBBuildBatch, AWSDevToolsQuery> {

    private static final String STARTING_TOKEN = StringUtils.EMPTY;

    private final AWSDevToolsClientFactory clientFactory;
    private final AWSDevToolsEnrichmentService enrichmentService;

    /**
     * all arg constructor
     *
     * @param clientFactory     {@link AWSDevToolsClientFactory} for fetching the {@link AWSDevToolsClient}
     * @param enrichmentService {@link AWSDevToolsEnrichmentService} for enriching the build batches
     */
    public AWSDevToolsBuildBatchDataSource(AWSDevToolsClientFactory clientFactory,
                                           AWSDevToolsEnrichmentService enrichmentService) {
        this.clientFactory = clientFactory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<CBBuildBatch> fetchOne(AWSDevToolsQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the build batches from AWSDevTools based on {@link AWSDevToolsQuery}.
     * It makes calls to AWSDevTools using the {@link AWSDevToolsClient}.
     *
     * @param query {@link AWSDevToolsQuery} used to fetch the build batches
     * @return {@link Stream<Data<CBBuildBatch>>} containing all the fetched build batches
     * @throws FetchException If any error occurs while fetching the build batches
     */
    @Override
    public Stream<Data<CBBuildBatch>> fetchMany(AWSDevToolsQuery query) throws FetchException {
        AWSDevToolsClient client = clientFactory.get(query.getRegionIntegrationKey());
        return PaginationUtils.stream(STARTING_TOKEN, token -> getPageData(client, query, token));
    }

    /**
     * Fetches {@link BuildBatch} using the {@code query} and {@code token}. It puts the token for the next page
     * in the returned {@link PaginationUtils.CursorPageData} for fetching the next page.
     * Returns {@code null} when {@code token} is {@code null} (denotes end of pages).
     *
     * @param client {@link AWSDevToolsClient} to make calls to codebuild
     * @param query  {@link AWSDevToolsQuery} for fetching the build batches
     * @param token  {@link String} token for the next page, must be equal to
     *               {@link AWSDevToolsBuildBatchDataSource#STARTING_TOKEN} for the first page
     * @return {@link PaginationUtils.CursorPageData} with the {@code token} for the next page
     */
    @Nullable
    private PaginationUtils.CursorPageData<Data<CBBuildBatch>> getPageData(AWSDevToolsClient client,
                                                                           AWSDevToolsQuery query,
                                                                           String token) {
        if (token == null)
            return null;
        AWSDevToolsQuery tokenQuery = STARTING_TOKEN.equals(token) ? query :
                AWSDevToolsQuery.builder()
                        .regionIntegrationKey(query.getRegionIntegrationKey())
                        .token(token)
                        .from(null)
                        .to(query.getTo())
                        .build();
        try {
            ListBuildBatchesResult listBuildBatchesResult = client.listBuildBatches(tokenQuery.getToken());
            String nextToken = listBuildBatchesResult.getNextToken();
            log.debug("getPageData: received next token for integration key: {} as {}", query.getIntegrationKey(), nextToken);
            List<BuildBatch> buildBatches = client.getBuildBatches(query, listBuildBatchesResult.getIds());
            List<CBBuildBatch> cbBuildBatches = enrichmentService.enrichBuildBatches(client, query.getIntegrationKey(),
                    client.getRegion(), buildBatches);
            return PaginationUtils.CursorPageData.<Data<CBBuildBatch>>builder()
                    .data(CollectionUtils.emptyIfNull(cbBuildBatches).stream()
                            .map(BasicData.mapper(CBBuildBatch.class))
                            .collect(Collectors.toList()))
                    .cursor(nextToken)
                    .build();
        } catch (AWSCodeBuildException | AWSDevToolsClientException e) {
            log.error("getPageData: encountered AWSDevTools client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("encountered AWSDevTools client exception for " + query.getIntegrationKey(), e);
        }

    }
}
