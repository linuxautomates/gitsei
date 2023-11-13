package io.levelops.ingestion.agent.utils;

import org.jasypt.util.text.AES256TextEncryptor;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.TextEncryptor;

public class EncryptionUtils {
    public static String encrypt(String textToEncrypt, String password) {
        return buildTextEncryptor(password).encrypt(textToEncrypt);
    }

    public static String decrypt(String textToDecrypt, String password) {
        return buildTextEncryptor(password).decrypt(textToDecrypt);
    }

    private static TextEncryptor buildTextEncryptor(String password) {
        var textEncryptor = new AES256TextEncryptor();
        textEncryptor.setPasswordCharArray(password.toCharArray());
        return textEncryptor;
    }
}
