package io.levelops.auth.auth.saml;

import com.onelogin.saml2.Auth;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import com.onelogin.saml2.util.Constants;
import io.levelops.commons.databases.models.database.SamlConfig;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.CommaListSplitter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class SamlService {
    private final URL acsUrl;
    private final String spId;
    private final String alternateSpId;
    private final Set<String> pingOneSSOTenants;

    public SamlService(String acsUrl, String spId, String alternateSpId, String PingOneSSOTenants) throws MalformedURLException {
        this.acsUrl = new URL(acsUrl);
        this.spId = spId;
        this.alternateSpId = alternateSpId;
        pingOneSSOTenants = CommaListSplitter.splitToStream(PingOneSSOTenants)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private Saml2Settings generateSaml2Settings(String company, SamlConfig config, String spId)
            throws CertificateException, MalformedURLException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate =
                (X509Certificate) certFactory
                        .generateCertificate(new ByteArrayInputStream(Base64.getMimeDecoder().decode(config.getIdpCert())));

        Map<String, Object> samlData = new HashMap<>();
        samlData.put("onelogin.saml2.sp.entityid", spId);
        samlData.put("onelogin.saml2.sp.assertion_consumer_service.url", acsUrl);
        samlData.put("onelogin.saml2.security.want_xml_validation", true);
        samlData.put("onelogin.saml2.sp.assertion_consumer_service.binding", Constants.BINDING_HTTP_POST);
        samlData.put("onelogin.saml2.strict", true);
        samlData.put("onelogin.saml2.sp.nameidformat", Constants.NAMEID_EMAIL_ADDRESS);
        samlData.put("onelogin.saml2.idp.entityid", config.getIdpId());
        samlData.put("onelogin.saml2.idp.x509cert", certificate);
        samlData.put("onelogin.saml2.idp.single_sign_on_service.url", new URL(config.getIdpSsoUrl()));
        //wants signed samlresponse, logout request, and logout response
        samlData.put("onelogin.saml2.security.want_messages_signed", true);
        //this is because we dont store state on server side today.
        samlData.put("onelogin.saml2.security.reject_unsolicited_responses_with_inresponseto", false);

        if(pingOneSSOTenants.contains(company)){
            samlData.put("onelogin.saml2.sp.nameidformat", Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        }

        SettingsBuilder builder = new SettingsBuilder();
        return builder.fromValues(samlData).build();
    }

    public void startAuthFlow(String company, SamlConfig config, HttpServletRequest request,
                              HttpServletResponse response)
            throws SettingsException, IOException, CertificateException {
        String relayState = new String(Base64.getEncoder().encode(company.getBytes()));
        try {
            Auth auth = new Auth(generateSaml2Settings(company, config, spId), request, response);

            //relaystate will contain company.
            //forceauthn is to not require relogin for user everytime
            //passive means minimal user interaction
            //nameid policy we want is emailaddress so that the idp return email address that we can use to get user.
            auth.login(relayState, false, false, true);
        } catch (Exception e) {
            if (alternateSpId == null || alternateSpId.isEmpty()) {
                log.error("Failed to authenticate with {} SP Entity Id", spId);
                log.error("Alternate SP Entity Id is not provided");
                throw e;
            }
            log.info("Using {} alternate SP Entity Id for authentication ", alternateSpId);
            Auth auth = new Auth(generateSaml2Settings(company, config, alternateSpId), request, response);
            auth.login(relayState, false, false, true);
        }
    }

    //this only returns email if saml response was valid.
    public User handleSamlResponse(String company, SamlConfig config, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        Auth auth = new Auth(generateSaml2Settings(company, config, spId), request, response);
        auth.processResponse();
        if (!auth.isAuthenticated()) {
            if (alternateSpId != null && !alternateSpId.isEmpty()) {
                log.info("User auth failed in first attempt.");
                log.error("Failing at first for samlResponse: {}", request.getParameter("SAMLResponse"));
                List<String> errors = auth.getErrors();
                if (!errors.isEmpty()) {
                    log.error("Errors with saml response: {}", errors);
                }
                log.info("Using {} alternate SP Entity Id to process response ", alternateSpId);
                auth = new Auth(generateSaml2Settings(company, config, alternateSpId), request, response);
                auth.processResponse();
            }
            if (!auth.isAuthenticated()) {
                log.info("User auth failed in second attempt.");
                List<String> errors = auth.getErrors();
                if (!errors.isEmpty()) {
                    log.error("Errors with saml response: {}", errors);
                }
                log.error("Failing again for samlResponse: {}", request.getParameter("SAMLResponse"));
                return null;
            }
        }
        log.info("SAML Attributes: {}", DefaultObjectMapper.writeAsPrettyJson(auth.getAttributes()));
        return User.builder()
                .email(auth.getNameId())
                .lastName(extractAttribute(auth::getAttribute, "LastName", "lastName").orElse(auth.getNameId()))
                .firstName(extractAttribute(auth::getAttribute, "FirstName", "firstName").orElse(auth.getNameId()))
                .build();
    }

    /**
     * Given a list of attribute names, returns the first non-blank value.
     * @param getAttribute function pointer to allow for testing since Auth is not easily mock-able
     * @param attributeNames names to try out in order
     */
    protected static Optional<String> extractAttribute(Function<String, Collection<String>> getAttribute, String... attributeNames) {
        for (String name : attributeNames) {
            String value = extractAttributeFirstValue(getAttribute, name);
            if (StringUtils.isNotBlank(value)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    @Nullable
    protected static String extractAttributeFirstValue(Function<String, Collection<String>> getAttribute, String attributeName) {
        return IterableUtils.getFirst(getAttribute.apply(attributeName)).orElse(null);
    }

}
