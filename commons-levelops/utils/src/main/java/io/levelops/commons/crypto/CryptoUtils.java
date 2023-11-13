/*
 * Copyright 2018 Fizzed, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.levelops.commons.crypto;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class CryptoUtils {

    static {
        // allows reading of different formats of PKCS (Java only supports PKCS8 by default)
        // requires bcpkix-jdk15on dependency
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider()
        );
    }

    static private final String KEY_RSA = "RSA";
    static private final String BEGIN_PKCS8 = "-----BEGIN PRIVATE KEY-----";
    static private final String END_PKCS8 = "-----END PRIVATE KEY-----";
    static private final String BEGIN_PKCS1 = "-----BEGIN RSA PRIVATE KEY-----";
    static private final String END_PKCS1 = "-----END RSA PRIVATE KEY-----";

    public static PrivateKey loadRSAPrivateKey(String content) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] bytes = parseBase64PrivateKey(content);

        final KeyFactory keyFactory = KeyFactory.getInstance(KEY_RSA);

        final EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);

        return keyFactory.generatePrivate(keySpec);
    }

    private static byte[] parseBase64PrivateKey(String content) {
        /**
         * TODO test and replace with
         *         return new PemReader(new StringReader(content)).readPemObject().getContent();
         */
        content = content.trim();

        // chop off pkcs#8 begin/end header (if it exists)
        if (content.startsWith(BEGIN_PKCS8)) {
            if (!content.endsWith(END_PKCS8)) {
                throw new IllegalArgumentException("Key content did not include " + END_PKCS8);
            }
            content = content.replace(BEGIN_PKCS8, "");
            content = content.replace(END_PKCS8, "");
            content = content.trim();
        }

        // chop off pkcs#1 begin/end header (if it exists)
        if (content.startsWith(BEGIN_PKCS1)) {
            if (!content.endsWith(END_PKCS1)) {
                throw new IllegalArgumentException("Key content did not include " + END_PKCS1);
            }
            content = content.replace(BEGIN_PKCS1, "");
            content = content.replace(END_PKCS1, "");
            content = content.trim();
        }

        return Base64.getMimeDecoder().decode(content);
    }
}