package io.levelops.commons.client.oauth.oauth1;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import io.levelops.commons.crypto.CryptoUtils;
import lombok.Getter;
import okio.Buffer;
import okio.ByteString;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

@Getter
public enum Oauth1SigningMethod {
    HMAC_SHA1("HMAC-SHA1") {
        @Override
        public String sign(Oauth1Credentials credentials, Buffer data) {
            String signingKey = ESCAPER.escape(credentials.getConsumerSecret()) + "&" + ESCAPER.escape(credentials.getAccessSecret());

            SecretKeySpec keySpec = new SecretKeySpec(signingKey.getBytes(), "HmacSHA1");
            Mac mac;
            try {
                mac = Mac.getInstance("HmacSHA1");
                mac.init(keySpec);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new IllegalStateException(e);
            }
            byte[] result = mac.doFinal(data.readByteArray());
            return ByteString.of(result).base64();
        }
    },
    RSA_SHA1("RSA-SHA1") {
        @Override
        public String sign(Oauth1Credentials credentials, Buffer data) {
            try {
                final Signature signature = Signature.getInstance("SHA1withRSA");

                PrivateKey privateKey = CryptoUtils.loadRSAPrivateKey(credentials.getConsumerSecret());

                final byte[] dataBytes = data.readByteArray();

                signature.initSign(privateKey);
                signature.update(dataBytes);

                byte[] signatureBytes = signature.sign();

                return Base64.getMimeEncoder().encodeToString(signatureBytes).trim();
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private static final Escaper ESCAPER = UrlEscapers.urlFormParameterEscaper();

    private final String value;

    Oauth1SigningMethod(String value) {
        this.value = value;
    }

    public abstract String sign(Oauth1Credentials credentials, Buffer data);

}
