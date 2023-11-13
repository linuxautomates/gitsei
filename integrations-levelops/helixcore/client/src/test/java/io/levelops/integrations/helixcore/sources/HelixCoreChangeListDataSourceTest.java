package io.levelops.integrations.helixcore.sources;

import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.helixcore.client.HelixCoreClient;
import io.levelops.integrations.helixcore.client.HelixCoreClientException;
import io.levelops.integrations.helixcore.client.HelixCoreClientFactory;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import io.levelops.integrations.helixcore.services.HelixCoreChangeListFetchService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class HelixCoreChangeListDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    private static final Date from = new GregorianCalendar(2020, Calendar.FEBRUARY, 11).getTime();
    private static final Date between = new GregorianCalendar(2020, Calendar.FEBRUARY, 12).getTime();
    private static final Date to = new GregorianCalendar(2020, Calendar.FEBRUARY, 13).getTime();

    HelixCoreChangeListDataSource dataSource;

    @Before
    public void setup() throws HelixCoreClientException {
        HelixCoreClient helixcoreClient = Mockito.mock(HelixCoreClient.class);
        HelixCoreClientFactory helixcoreClientFactory = Mockito.mock(HelixCoreClientFactory.class);
        HelixCoreChangeListFetchService helixCoreChangeListFetchService = Mockito.mock(HelixCoreChangeListFetchService.class);
        dataSource = new HelixCoreChangeListDataSource(helixcoreClientFactory, helixCoreChangeListFetchService);
        when(helixcoreClientFactory.get(TEST_KEY)).thenReturn(helixcoreClient);
        List<HelixCoreChangeList> changeLists = List.of(
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(1).build(),
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(2).build(),
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(3).build(),
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(4).build(),
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(5).build());
        when(helixCoreChangeListFetchService
                .fetchChangeLists(helixcoreClient, DateUtils.toInstant(from), DateUtils.toInstant(to)))
                .thenReturn(changeLists.stream());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(HelixCoreIterativeQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<HelixCoreChangeList>> accounts = dataSource.fetchMany(
                HelixCoreIterativeQuery.builder().integrationKey(TEST_KEY).from(from).to(to).build()).collect(Collectors.toList());
        assertThat(accounts).hasSize(5);
    }
}
