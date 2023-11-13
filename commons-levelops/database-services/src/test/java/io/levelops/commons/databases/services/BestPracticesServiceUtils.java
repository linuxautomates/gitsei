package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.BestPracticesItem;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BestPracticesServiceUtils {
    public static BestPracticesItem createBestPracticesItem(BestPracticesService bestPracticesService, String company, int i) throws SQLException {
        BestPracticesItem item = BestPracticesItem.builder()
                .name("test-" + i)
                .type(BestPracticesItem.BestPracticeType.FILE)
                .value("file-id-" +i)
                .metadata("file-name-" + i)
                .build();
        String id = bestPracticesService.insert(company, item);
        Assert.assertNotNull(id);
        return item.toBuilder().id(UUID.fromString(id)).build();
    }
    public static List<BestPracticesItem> createBestPracticesItems(BestPracticesService bestPracticesService, String company, int n) throws SQLException {
        List<BestPracticesItem> bestPracticesItems = new ArrayList<>();
        for(int i=0; i<n; i++) {
            bestPracticesItems.add(createBestPracticesItem(bestPracticesService, company, i));
        }
        return bestPracticesItems;
    }
}
