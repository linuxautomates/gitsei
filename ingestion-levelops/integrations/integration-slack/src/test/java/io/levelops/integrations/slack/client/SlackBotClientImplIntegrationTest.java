package io.levelops.integrations.slack.client;

import com.google.common.base.MoreObjects;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.slack.models.SlackApiChannel;
import io.levelops.integrations.slack.models.SlackApiChannelResponse;
import io.levelops.integrations.slack.models.SlackApiChatMessagePermalinkResponse;
import io.levelops.integrations.slack.models.SlackApiFileUploadResponse;
import io.levelops.integrations.slack.models.SlackApiResponse;
import io.levelops.integrations.slack.models.SlackApiUser;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class SlackBotClientImplIntegrationTest {

    private SlackBotClientFactory clientFactory;
    private static final IntegrationKey KEY = IntegrationKey.builder().tenantId("coke").integrationId("slack1").build();

    @Before
    public void setUp() throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient();
        clientFactory = SlackBotClientFactory.builder()
                .inventoryService(new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                        .oauthToken("coke", "slack1", "slack", null, null, null, null,
                                MoreObjects.firstNonNull(System.getenv("SLACK_TOKEN") , "token"))
                        .build()))
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .build();
    }

    @Test
    public void postMessage() throws SlackClientException {
        String text = "integration test `" + Instant.now().toString() + "`";
        clientFactory.get(KEY).postChatMessage("DP2TGV09G", text, null);
    }

    @Test
    public void postMessageChannel() throws SlackClientException {
        String text = "integration test `" + Instant.now().toString() + "`";
        // bot needs to be invited to channel (using /invite @levelops or just mention it using @levelops and click invite)
        clientFactory.get(KEY).postChatMessage("slackbot-test-private", text, null);
    }

    @Test
    public void postInteractiveMessage() throws SlackClientException, IOException {
        //String text = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_message_blocks.json");
        String text = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_message_blocks_1.json");
        String attachmentsText = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_message_attachment_1.json");
        //clientFactory.get(KEY).postChatInteractiveMessage("US4JC7ZM5", text, "mogambo");
        clientFactory.get(KEY).postChatInteractiveMessage("meghana@levelops.io", text, "mogambo");
    }

    @Test
    public void postInteractiveMessageChannel() throws SlackClientException, IOException {
        //String text = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_message_blocks.json");
        String text = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_message_blocks_2.json");
        String attachmentsText = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_message_attachment_1.json");
        // bot needs to be invited to channel (using /invite @levelops or just mention it using @levelops and click invite)
        //clientFactory.get(KEY).postChatInteractiveMessage("slackbot-test-private-va", text, "mogambo");
        clientFactory.get(KEY).postChatInteractiveMessage("slackbot-test-private-va-4-pvt", text, "mogambo");
    }

    @Test
    public void postInteractiveMessageChannelAssessment1() throws SlackClientException, IOException {
        //String text = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_message_blocks.json");
        String text = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_msg_assessment_1.json");
        // bot needs to be invited to channel (using /invite @levelops or just mention it using @levelops and click invite)
        //clientFactory.get(KEY).postChatInteractiveMessage("slackbot-test-private-va", text, "mogambo");
        clientFactory.get(KEY).postChatInteractiveMessage("slackbot-test-private-va-4-pvt", text, "mogambo");
    }

    @Test
    public void postInteractiveMessageChannelAssessment2() throws SlackClientException, IOException {
        //String text = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_message_blocks.json");
        String text = ResourceUtils.getResourceAsString("slack/slack_chat_interactive_msg_assessment_2.json");
        // bot needs to be invited to channel (using /invite @levelops or just mention it using @levelops and click invite)
        //clientFactory.get(KEY).postChatInteractiveMessage("slackbot-test-private-va", text, "mogambo");
        clientFactory.get(KEY).postChatInteractiveMessage("slackbot-test-private-va-4-pvt", text, "mogambo");
    }


    @Test
    public void lookupUser() throws SlackClientException {
        Optional<SlackApiUser> slackApiUser = clientFactory.get(KEY).lookupUserByEmail("maxime@levelops.io");
        DefaultObjectMapper.prettyPrint(slackApiUser.get());

        slackApiUser = clientFactory.get(KEY).lookupUserByEmail("missing");
        DefaultObjectMapper.prettyPrint(slackApiUser);
    }

    @Test
    public void openImChannel() throws SlackClientException {
        Optional<SlackApiChannel> output = clientFactory.get(KEY).openImChannel("UMC5G1JLX");
        DefaultObjectMapper.prettyPrint(output.get());
    }

    @Test
    public void lookupUserByUserIdFound() throws SlackClientException {
        Optional<SlackApiUser> slackApiUser = clientFactory.get(KEY).lookupUserById("US4JC7ZM5");
        DefaultObjectMapper.prettyPrint(slackApiUser.get());

        SlackApiUser.Profile profile = SlackApiUser.Profile.builder()
                .title("").phone("")
                .realName("Viraj Ajgaonkar").realNameNormalized("Viraj Ajgaonkar")
                .displayName("Viraj Ajgaonkar").displayNameNormalized("Viraj Ajgaonkar")
                .email("viraj@levelops.io").team("TM3U03S49")
                .build();
        SlackApiUser expected = SlackApiUser.builder()
                .id("US4JC7ZM5").teamId("TM3U03S49").name("viraj").deleted(false).color("bb86b7")
                .realName("Viraj Ajgaonkar")
                .tz("America/Los_Angeles").tzLabel("Pacific Daylight Time")
                .isAdmin(false).isOwner(false).isPrimaryOwner(false).isRestricted(false).isUltraRestricted(false)
                .isBot(false).isAppUser(false).profile(profile)
                .build();

        Assert.assertEquals(expected, slackApiUser.get());
    }

    @Test
    public void lookupUserByUserIdNotFound() throws SlackClientException {
        Optional<SlackApiUser> slackApiUser = clientFactory.get(KEY).lookupUserById("US4JC7ZM6");
        Assert.assertTrue(slackApiUser.isEmpty());
    }

    @Test
    public void testJoinChannel()  {
        SlackApiChannelResponse apiChannelResponse = null;
        try {
            apiChannelResponse = clientFactory.get(KEY).joinChannel("CMA9GQVA4");
            Assert.fail("SlackClientException expected");
        } catch (SlackClientException e) {
            SlackApiResponse slackApiResponse = e.getApiResponse();
            String error = (String) slackApiResponse.getDynamicProperties().getOrDefault("error", "");
            Assert.assertEquals(error, "invalid_name_specials");
        }
    }

    @Test
    public void testJoinConversation() throws SlackClientException {
        SlackApiChannelResponse apiChannelResponse = clientFactory.get(KEY).joinConversation("CMA9GQVA4");
        Assert.assertNotNull(apiChannelResponse);
    }

    @Test
    public void testFileUpload() throws IOException, SlackClientException {
        String fileContent = ResourceUtils.getResourceAsString("slack_files/slack_file_upload_snippet_file.txt");
        SlackApiFileUploadResponse slackApiFileUpload = clientFactory.get(KEY).fileUpload(List.of("slackbot-test-private-va-4-pvt"), "Stage 2 Rule 1 Test Now1.txt", fileContent, "1602031256.000100");
        Assert.assertTrue(StringUtils.isNotBlank(slackApiFileUpload.getFile().getId()));
    }

    @Test
    public void testGetChatMessagePermalink() throws SlackClientException {
        SlackApiChatMessagePermalinkResponse response = clientFactory.get(KEY).getChatMessagePermalink("G01C14VKRJP", "1602592157.000100");
        SlackApiChatMessagePermalinkResponse expected = SlackApiChatMessagePermalinkResponse.builder()
                .permalink("https://levelopsworkspace.slack.com/archives/G01C14VKRJP/p1602592157000100")
                .channel("G01C14VKRJP")
                .build();
        Assert.assertEquals(response, expected);
    }
}