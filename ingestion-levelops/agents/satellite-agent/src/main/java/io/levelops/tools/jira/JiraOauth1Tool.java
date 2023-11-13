package io.levelops.tools.jira;

import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class JiraOauth1Tool {

    public static void main(String[] args) throws Exception {
        // Usage (step 1): request_token <property file containing: base_url, consumer_key, private_key>
        // Usage (step 2): access_token <property file containing: base_url, consumer_key, private_key, requestToken, verificationCode>
        if (args.length != 2) {
            usage();
        }
        String command = args[0];
        String propertyFilePath = args[1];
        Properties properties = readPropertyFile(args[1]);
        String baseUrl = getProperty(properties, "base_url");
        String consumerKey = getProperty(properties, "consumer_key");
        String privateKey = getProperty(properties, "private_key");

        JiraOauth1Helper jiraOauth1 = new JiraOauth1Helper(baseUrl);

        switch (command.trim().toLowerCase()) {
            case "request_token": {
                String requestToken = jiraOauth1.getRequestToken(consumerKey, privateKey);
                System.out.println("\n");
                System.out.println("Request token:\t\t\t" + requestToken);
                String requestTokenAuthUrl = jiraOauth1.buildRequestTokenAuthUrl(requestToken);
                System.out.println("Retrieved request token. To authorize it, go to: " + requestTokenAuthUrl);

                properties.put("request_token", requestToken);
                properties.put("verification_code", "");
                writePropertyFile(properties, propertyFilePath);
                break;
            }
            case "access_token": {
                String requestToken = getProperty(properties, "request_token");
                String verificationCode = getProperty(properties, "verification_code");
                String accessToken = jiraOauth1.getAccessToken(consumerKey, privateKey, requestToken, verificationCode);

                System.out.println("\n");
                System.out.println("Access token:\t\t\t" + accessToken);

                properties.put("access_token", accessToken);
                writePropertyFile(properties, propertyFilePath);
                break;
            }
            default:
                fatalError("Unknown command: '" + command + "'");
        }
    }

    private static void usage() {
        System.err.println("" +
                "Usage (step 1): request_token <property file>\n" +
                "                The property file must contain the following parameters:\n" +
                "                 - base_url\n" +
                "                 - consumer_key\n" +
                "                 - private_key\n");
        System.err.println("" +
                "Usage (step 2): access_token <property file>\n" +
                "                The property file must contain the following parameters:\n" +
                "                 - base_url\n" +
                "                 - consumer_key\n" +
                "                 - private_key\n" +
                "                 - request_token\n" +
                "                 - verification_code\n");
        System.err.println("--- Property file example ---\n" +
                "base_url=http://jira.acme.com\n" +
                "consumer_key=OauthKey\n" +
                "private_key=\n\n");
        System.exit(0);
    }

    private static void fatalError(String msg) {
        System.err.println("[ERROR] " + msg);
        System.err.println();
        usage();
    }

    private static String getProperty(Properties prop, String key) {
        String value = prop.getProperty(key);
        if (StringUtils.isBlank(value)) {
            fatalError("Could not find property: " + key);
        }
        return value;
    }

    private static Properties readPropertyFile(String path) throws IOException {
        try {
            InputStream inputStream = new FileInputStream(path);
            Properties prop = new Properties();
            prop.load(inputStream);
            return prop;
        } catch (FileNotFoundException e) {
            fatalError("Could not find property file: " + path);
            throw new RuntimeException(e); // should have exited by now
        }
    }

    private static void writePropertyFile(Properties properties, String path) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            properties.store(outputStream, null);
        }
    }

}
