package io.levelops.integrations.tenable.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.Asset;
import io.levelops.integrations.tenable.models.ExportResponse;
import io.levelops.integrations.tenable.models.ExportStatusResponse;
import io.levelops.integrations.tenable.models.TenableScanQuery;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tenable's implementation of the {@link DataSource}. This class is used for fetching assets from tenable.
 */
@Log4j2
public class TenableAssetDataSource implements DataSource<Asset, TenableScanQuery> {

    private final TenableClientFactory clientFactory;

    public TenableAssetDataSource(TenableClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<Asset> fetchOne(TenableScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<Asset>> fetchMany(TenableScanQuery query) throws FetchException {
        TenableClient tenableClient = clientFactory.get(query.getIntegrationKey());
        ExportResponse exportResponse = tenableClient.exportAssets(query);
        String exportUUID = exportResponse.getExportUUID();
        ChunkPageData initialChunkPageData = new ChunkPageData(exportUUID, null,
                false, null, new ArrayList<>(), new ArrayList<>());
        ChunkPageSupplier chunkPageSupplier = new ChunkPageSupplier
                (initialChunkPageData, chunkPageData -> getAssetData(tenableClient, chunkPageData));
        return PaginationUtils.stream(chunkPageSupplier, ChunkPageSupplier.predicate)
                .filter(chunkPageData -> chunkPageData.getChunkId() != null)
                .map(chunkPageData -> {
                    try {
                        log.debug("Downloading asset chunk with id: {} for uuid: {}",
                                chunkPageData.getChunkId(), chunkPageData.getUuid());
                        List<Asset> assets = tenableClient.downloadAssetChunk(chunkPageData.getUuid(), chunkPageData.getChunkId());
                        chunkPageData.getChunksProcessed().add(chunkPageData.getChunkId());
                        return assets.stream().map(BasicData.mapper(Asset.class)).collect(Collectors.toList());
                    } catch (TenableClientException e) {
                        log.error("Encountered tenable client error for integration key: "
                                + query.getIntegrationKey() + " as : " + e.getMessage(), e);
                        throw new RuntimeStreamException("Encountered tenable client error for integration key: " + query.getIntegrationKey(), e);
                    }
                })
                .flatMap(List::stream);
    }

    private ChunkPageData getAssetData(TenableClient tenableClient, ChunkPageData chunkPageData) {
        try {
            ExportStatusResponse exportStatus = tenableClient.getAssetsExportStatus(chunkPageData.uuid);
            Set<Integer> chunksAvailableSet = new HashSet<>(exportStatus.getChunksAvailable());
            Set<Integer> chunksProcessedSet = new HashSet<>(chunkPageData.getChunksProcessed());
            log.debug("Available chunk size: {}, downloaded chunk size: {}",
                    chunksAvailableSet.size(), chunksProcessedSet.size());
            chunksAvailableSet.removeAll(chunksProcessedSet);
            if (chunksAvailableSet.size() > 0) {
                Integer chunkId = chunksAvailableSet.iterator().next();
                return ChunkPageData.builder()
                        .uuid(chunkPageData.getUuid())
                        .done(chunkPageData.getDone())
                        .chunkId(chunkId)
                        .chunksAvailable(exportStatus.getChunksAvailable())
                        .chunksProcessed(chunkPageData.getChunksProcessed())
                        .status(exportStatus.getStatus())
                        .build();
            } else {
                return ChunkPageData.builder()
                        .uuid(chunkPageData.getUuid())
                        .done(chunkPageData.getDone())
                        .chunkId(null)
                        .chunksAvailable(exportStatus.getChunksAvailable())
                        .chunksProcessed(chunkPageData.getChunksProcessed())
                        .status(exportStatus.getStatus())
                        .build();
            }
        } catch (TenableClientException e) {
            log.error("Encountered tenable client exception. Reason: " + e.getMessage(), e);
            throw new RuntimeStreamException("Encountered tenable client exception", e);
        }
    }
}
