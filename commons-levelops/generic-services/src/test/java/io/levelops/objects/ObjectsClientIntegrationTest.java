package io.levelops.objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.models.database.automation_rules.ObjectTypeDTO;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ObjectsClientIntegrationTest {
    private static final String COMPANY = "foo";
    private final ObjectMapper objectMapper = DefaultObjectMapper.get();
    private ObjectsClient client;

    @Before
    public void setUp() throws Exception {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(logging).build();
        client = new ObjectsClient(okHttpClient, objectMapper, "http://localhost:8080");
    }

    @Test
    public void testListObjectTypes() throws IOException {
        DbListResponse<String> response = client.listObjectTypes(COMPANY, DefaultListRequest.builder().build());
        Assert.assertEquals(2, response.getRecords().size());
    }

    @Test
    public void testGetFieldsForObjectType() throws IOException {
        List<String> fields = client.getFieldsForObjectType(COMPANY, ObjectType.JIRA_ISSUE);
        Assert.assertEquals(4, fields.size());
    }

    @Test
    public void testListObjectTypeDetails() throws IOException {
        DbListResponse<ObjectTypeDTO> response = client.listObjectTypeDetails(COMPANY, DefaultListRequest.builder().build());
        Assert.assertEquals(2, response.getRecords().size());
    }
}