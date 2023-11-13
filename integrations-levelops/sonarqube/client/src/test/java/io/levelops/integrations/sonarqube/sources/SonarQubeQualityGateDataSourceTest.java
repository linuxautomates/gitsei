package io.levelops.integrations.sonarqube.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.QualityGate;
import io.levelops.integrations.sonarqube.models.QualityGateResponse;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class SonarQubeQualityGateDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    SonarQubeQualityGateDataSource dataSource;

    @Before
    public void setup() throws SonarQubeClientException {
        SonarQubeClient sonarQubeClient = Mockito.mock(SonarQubeClient.class);
        SonarQubeClientFactory sonarQubeClientFactory = Mockito.mock(SonarQubeClientFactory.class);
        dataSource = new SonarQubeQualityGateDataSource(sonarQubeClientFactory);
        when(sonarQubeClientFactory.get(TEST_KEY)).thenReturn(sonarQubeClient);
        List<QualityGate> qualityGates = List.of(
                QualityGate.builder().id("1").build(),
                QualityGate.builder().id("2").build(),
                QualityGate.builder().id("3").build(),
                QualityGate.builder().id("4").build(),
                QualityGate.builder().id("5").build());
        QualityGateResponse qualityGateResponse = QualityGateResponse.builder().qualitygates(qualityGates).build();
        when(sonarQubeClient.getQualityGates()).thenReturn(qualityGateResponse);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(SonarQubeIterativeScanQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<QualityGate>> components = dataSource.fetchMany(
                SonarQubeIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .build()).collect(Collectors.toList());
        assertThat(components).hasSize(5);
    }
}