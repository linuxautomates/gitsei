package io.levelops.integrations.helixcore.sources;

import com.perforce.p4java.impl.generic.core.file.FileSpec;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class HelixCoreChangeListDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    private static final Date from = new GregorianCalendar(2020, Calendar.FEBRUARY, 11).getTime();
    private static final Date between = new GregorianCalendar(2020, Calendar.FEBRUARY, 12).getTime();
    private static final Date to = new GregorianCalendar(2020, Calendar.FEBRUARY, 13).getTime();
    private static final int pageSizeInDays = 1;
    private static final ZoneId ZONE_ID = ZoneId.of("UTC");

    HelixCoreChangeListDataSource dataSource;

    @Before
    public void setup() throws HelixCoreClientException {
        HelixCoreClient helixcoreClient = Mockito.mock(HelixCoreClient.class);
        HelixCoreClientFactory helixcoreClientFactory = Mockito.mock(HelixCoreClientFactory.class);
        HelixCoreChangeListFetchService helixCoreChangeListFetchService = Mockito.mock(HelixCoreChangeListFetchService.class);
        dataSource = new HelixCoreChangeListDataSource(helixcoreClientFactory, helixCoreChangeListFetchService, 1000000, ZONE_ID);
        when(helixcoreClientFactory.get(TEST_KEY)).thenReturn(helixcoreClient);
        List<HelixCoreChangeList> changeLists = List.of(
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(1).build(),
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(2).build(),
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(3).build(),
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(4).build(),
                HelixCoreChangeList.builder().lastUpdatedAt(between).id(5).build());
        when(helixCoreChangeListFetchService
                .fetchChangeLists(helixcoreClient,DateUtils.toInstant(from), DateUtils.toInstant(to), pageSizeInDays, 1000000, ZONE_ID))
                .thenReturn(changeLists.stream());
    }

    @Test
    public void fetchOne() {
        assertThat(new FileSpec("@42033507").getChangelistId()).isEqualTo(42033507);
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<HelixCoreChangeList>> accounts = dataSource.fetchMany(
                HelixCoreIterativeQuery.builder().integrationKey(TEST_KEY).from(from).to(to).build())
                .collect(Collectors.toList());
        assertThat(accounts).hasSize(5);
    }

    @Test
    public void test() {
        Instant from = Instant.ofEpochMilli(1646267100000l);
        ZonedDateTime zdt = from.atZone(ZoneId.of("America/Los_Angeles"));
        System.out.println(from);
        System.out.println(from.getEpochSecond());

        System.out.println("zdt " + zdt);
        System.out.println("zdt " + zdt.toLocalDate());
//        System.out.println(zdt.toLocalDate());

        LocalDate fromDtUTC = LocalDate.ofInstant(from, ZoneOffset.UTC);
        System.out.println("fromDtUTC " + fromDtUTC);
//        System.out.println(fromDt);
    }
}
