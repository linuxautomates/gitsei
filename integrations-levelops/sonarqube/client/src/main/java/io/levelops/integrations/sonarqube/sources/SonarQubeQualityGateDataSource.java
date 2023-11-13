package io.levelops.integrations.sonarqube.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.QualityGate;
import io.levelops.integrations.sonarqube.models.QualityGateResponse;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

/**
 * Sonarqube's implementation of the {@link DataSource}. This class can be used to fetch quality gates data from Sonarqube.
 */
@Log4j2
public class SonarQubeQualityGateDataSource implements DataSource<QualityGate, SonarQubeIterativeScanQuery> {

    private final SonarQubeClientFactory sonarQubeClientFactory;

    public SonarQubeQualityGateDataSource(SonarQubeClientFactory sonarQubeClientFactory) {
        this.sonarQubeClientFactory = sonarQubeClientFactory;
    }

    @Override
    public Data<QualityGate> fetchOne(SonarQubeIterativeScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<QualityGate>> fetchMany(SonarQubeIterativeScanQuery query)
            throws FetchException {
        SonarQubeClient sonarQubeClient = sonarQubeClientFactory.get(query.getIntegrationKey());
        QualityGateResponse qualityGateResponse = sonarQubeClient.getQualityGates();
        try {
            log.info("qualityGateResponse is : {} ",qualityGateResponse);
            return qualityGateResponse.getQualitygates().stream()
                    .map(BasicData.mapper(QualityGate.class));
        } catch (Exception e) {
            log.error("Encountered SonarQube client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("Encountered SonarQube client error for integration key: " +
                    query.getIntegrationKey(), e);
        }
    }
}