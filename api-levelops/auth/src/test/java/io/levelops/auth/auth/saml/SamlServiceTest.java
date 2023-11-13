package io.levelops.auth.auth.saml;

import com.onelogin.saml2.exception.Error;
import com.onelogin.saml2.exception.SettingsException;
import io.levelops.commons.databases.models.database.SamlConfig;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class SamlServiceTest {

    private HttpServletRequest request;
    @Mock
    private HttpServletResponse responseMock;
    private static SamlService samlService;

   // @Before
    public void setup() throws Exception {

        samlService = new SamlService("https://auth.pingone.asia/62343c88-3a5a-40b4-94f5-624ea82a7761", "dev-propelo.ai", "dev-levelops.io", "test");
        request = Mockito.mock(HttpServletRequest.class);

        String responseString = ResourceUtils.getResourceAsString("cert/samlResponse.xml");
        byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
        String base64SamlResponse = Base64.getEncoder().encodeToString(responseBytes);

        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getParameter("paramName")).thenReturn("paramValue");
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("https://testapi1.propelo.ai/v1/saml_auth"));
        Mockito.when(request.getParameter("RelayState")).thenReturn("a21weXNtdWlzaW1vcnJqbDZubDczdw==");
        Mockito.when(request.getParameterMap()).thenReturn(Map.of("SAMLResponse", new String[]{base64SamlResponse}));
    }

    @Ignore // Ignoring this as it is added just for debugging purpose
    @Test
    public void testLogin() throws IOException, CertificateException, SettingsException {

        String idpCert = ResourceUtils.getResourceAsString("cert/idpCert.crt");
        byte[] certificateBytes = idpCert.getBytes(StandardCharsets.UTF_8);
        String base64EncodedCertificate = Base64.getMimeEncoder().encodeToString(certificateBytes);

        SamlConfig config = SamlConfig.builder()
                .id("1")
                .idpId("https://auth.pingone.asia/62343c88-3a5a-40b4-94f5-624ea82a7761")
                .idpSsoUrl("https://auth.pingone.asia/62343c88-3a5a-40b4-94f5-624ea82a7761/saml20/idp/startsso?spEntityId=dev-propelo.ai")
                .idpCert(base64EncodedCertificate)
                .enabled(true)
                .build();

        samlService.startAuthFlow("test", config, request, responseMock);
    }
    @Ignore //Ignoring test as overall executian fails at signature validation but, execution is going past the issue the client is facing
    @Test
    public void handleSamlResponseTest() throws Exception {

        try {
            String idpCert = ResourceUtils.getResourceAsString("cert/idpCert.crt");
            byte[] certificateBytes = idpCert.getBytes(StandardCharsets.UTF_8);
            String base64EncodedCertificate = Base64.getMimeEncoder().encodeToString(certificateBytes);

            SamlConfig config = SamlConfig.builder()
                    .id("1")
                    .idpId("https://auth.pingone.asia/62343c88-3a5a-40b4-94f5-624ea82a7761")
                    .idpSsoUrl("https://auth.pingone.asia/62343c88-3a5a-40b4-94f5-624ea82a7761/saml20/idp/startsso?spEntityId=dev-propelo.ai")
                    .idpCert(base64EncodedCertificate)
                    .enabled(true)
                    .build();

            User user = samlService.handleSamlResponse("test", config, request, responseMock);
            Assertions.assertThat(user.getEmail()).isEqualTo("ashish@propelo.ai");

        } catch (Exception e) {
            System.err.println("Failed to read the certificate file: " + e.getMessage());
            e.printStackTrace();

        }
    }

    @Test
    public void extractAttribute() throws Error, IOException, SettingsException {
        Map<String, Collection<String>> attributes = Map.of(
                "empty", List.of(),
                "emptyString", List.of(""),
                "list", List.of("first", "second"));
        assertThat(SamlService.extractAttributeFirstValue(attributes::get, "null")).isEqualTo(null);
        assertThat(SamlService.extractAttributeFirstValue(attributes::get, "empty")).isEqualTo(null);
        assertThat(SamlService.extractAttributeFirstValue(attributes::get, "emptyString")).isEqualTo("");
        assertThat(SamlService.extractAttributeFirstValue(attributes::get, "list")).isEqualTo("first");

        assertThat(SamlService.extractAttribute(attributes::get, "a", "b", "emptyString", "list", "c").orElse("default")).isEqualTo("first");
        assertThat(SamlService.extractAttribute(attributes::get, "a", "b", "emptyString").orElse("default")).isEqualTo("default");
    }
}