package io.levelops.ingestion.agent;

import io.levelops.ingestion.agent.utils.EncryptionUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Decrypt {

    public static String decrypt(String text, String password) {
        return EncryptionUtils.decrypt(text, password);
    }

    public static void main(String[] args) throws IOException {

        // read file to encrypt
        if (args.length != 1) {
            System.err.println("ERROR: no input file");
            System.exit(1);
        }
        String path  = args[0];
        String textToDecrypt = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);

        // read password from std in
        Scanner scanner = new Scanner(System.in);
        String password = scanner.nextLine();
        scanner.close();

        // decrypt and output to std out
        String encryptedText = decrypt(textToDecrypt, password);
        System.out.println(encryptedText);
    }


}
