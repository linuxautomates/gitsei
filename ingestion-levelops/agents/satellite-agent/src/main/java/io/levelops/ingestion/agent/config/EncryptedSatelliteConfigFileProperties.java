package io.levelops.ingestion.agent.config;

import java.io.FileInputStream;
import java.io.IOException;

import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.levelops.ingestion.agent.utils.EncryptionUtils;

@Configuration
public class EncryptedSatelliteConfigFileProperties {

    @Bean
    @Profile("encrypted")
    public SatelliteConfigFileProperties satelliteConfigFileProperties(
            @Value("${ENCRYPTION_PASSWORD:}") String password,
            @Value("${CONFIG_FILE:config.yml}") String filePath) throws IOException {
        if (StringUtils.isBlank(password)) {
            System.err.println("#\n# To run the satellite in encrypted mode, you must pass the encryption password via the ENCRYPTION_PASSWORD environment variable.\n#");
            System.exit(1);
        }
        String textToDecrypt = IOUtils.toString(new FileInputStream(filePath));
        String decrypted = EncryptionUtils.decrypt(textToDecrypt, password);
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        return yamlMapper.readValue(decrypted, SatelliteConfigFileProperties.class);
    }

}
