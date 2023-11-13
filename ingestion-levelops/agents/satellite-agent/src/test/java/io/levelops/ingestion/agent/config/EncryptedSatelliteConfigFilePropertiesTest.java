package io.levelops.ingestion.agent.config;

import io.levelops.ingestion.agent.Decrypt;
import io.levelops.ingestion.agent.Encrypt;
import org.junit.Rule;
import org.junit.Test;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.rules.TemporaryFolder;


import java.io.BufferedWriter;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


public class EncryptedSatelliteConfigFilePropertiesTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testEncryptedConfig() throws IOException {
        String satellite = ResourceUtils.getResourceAsString("satellite.yml");
        String encrypted_file = Encrypt.encrypt(satellite, "steph-curry-is-goat");
        var f = temporaryFolder.newFile("satellite.yml.enc");
        System.out.println(f.getAbsolutePath());
        BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(f));
        writer.write(encrypted_file);
        writer.flush();
        writer.close();

        EncryptedSatelliteConfigFileProperties encryptedSatelliteConfigFileProperties = new EncryptedSatelliteConfigFileProperties();
        var prop = encryptedSatelliteConfigFileProperties.satelliteConfigFileProperties("steph-curry-is-goat", f.getAbsolutePath());
        assertThat(prop.integrations.get(0).getSatellite()).isTrue();
        assertThat(prop.integrations.get(0).getUserName()).isEqualTo("steph-curry");
    }
}
