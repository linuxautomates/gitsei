package io.levelops.integrations.gerrit.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.gerrit.models.AccountInfo;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountInfoTest {

    private static final String RESPONSE_FILE_NAME = "accounts.json";
    private static final int EXPECTED_NUM_ACCOUNTS = 2;

    @Test
    public void deSerialize() throws IOException {
        List<AccountInfo> response = DefaultObjectMapper.get()
                .readValue(AccountInfoTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(List.class, AccountInfo.class));
        assertThat(response).isNotNull();
        assertThat(response).hasSize(EXPECTED_NUM_ACCOUNTS);
    }
}
