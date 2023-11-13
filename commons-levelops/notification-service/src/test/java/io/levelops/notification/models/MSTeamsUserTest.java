package io.levelops.notification.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.notification.models.msteams.MSTeamsApiResponse;
import io.levelops.notification.models.msteams.MSTeamsUser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class MSTeamsUserTest {

    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialize() throws IOException {
        String user = ResourceUtils.getResourceAsString("msteams/user.json");
        MSTeamsApiResponse<MSTeamsUser> response = MAPPER.readValue(user,MSTeamsUser.getJavaType(MAPPER));
        Assert.assertNotNull(response.getValues());
        Assert.assertEquals("test patel", response.getValues().get(0).getDisplayName());
        Assert.assertEquals("testpatel@PROPCDS.onmicrosoft.com", response.getValues().get(0).getMail());
        Assert.assertEquals("testpatel@PROPCDS.onmicrosoft.com", response.getValues().get(0).getUserPrincipalName());
    }
}
