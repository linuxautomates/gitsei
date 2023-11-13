package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

public class BitbucketTeamTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile =  new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("bitbucket/BB_Api_List_Teams.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        BitbucketPaginatedResponse<BitbucketTeam> paginatedResponse = mapper.readValue(testFile, BitbucketPaginatedResponse.ofType(mapper, BitbucketTeam.class));
        Assert.assertNotNull(paginatedResponse);
        List<BitbucketTeam> bitbucketTeams = paginatedResponse.values;
        Assert.assertNotNull(bitbucketTeams);
        Assert.assertEquals(1, bitbucketTeams.size());

        BitbucketTeam apiTeam = BitbucketTeam.builder()
                .username("levelopstest")
                .displayName("levelopstest")
                .uuid("{82a01e22-1011-4ec6-90ba-d83f4eab9595}")
                .createdOn("2020-02-11T23:00:41.472828+00:00")
                .type("team")
                .has2faEnabled(null).build();

        Assert.assertEquals(bitbucketTeams.get(0), apiTeam);
    }
}