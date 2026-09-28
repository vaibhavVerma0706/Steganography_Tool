// This file is inside the "utils" folder/package
package utils;

// These are Java's built-in libraries — no downloads needed
// javax.crypto = Java's built-in encryption library
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

// java.security = Java's built-in security library
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

// java.util.Base64 = converts encrypted bytes to readable text
// Available since JDK 8 — compatible with JDK 17 and 21 ✅
import java.util.Base64;

public class AESEncryption {

    // -------------------------------------------------------
    // SALT — makes encryption stronger and harder to crack
    // Must be exactly 16 bytes long
    // Same salt is used for both encrypting and decrypting
    // -------------------------------------------------------
    private static final byte[] SALT = "StegoSalt123456!".getBytes();

    // -------------------------------------------------------
    // IV (Initialization Vector) — adds extra randomness
    // Must be exactly 16 bytes long for AES
    // Same IV is used for both encrypting and decrypting
    // -------------------------------------------------------
    private static final byte[] IV = "StegoIV12345678!".getBytes();

    // -------------------------------------------------------
    // generateKey() — converts your text password into
    // a proper 256-bit AES encryption key
    // PBKDF2WithHmacSHA256 = secure key generation algorithm
    // 65536 = number of times password is hashed (more = safer)
    // 256 = key size in bits (AES-256)
    // -------------------------------------------------------
    private static SecretKey generateKey(String password) throws Exception {
        // Get the key generation factory
        SecretKeyFactory factory = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256");

        // Define how to generate the key
        // password.toCharArray() = converts password string to char array
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(), // your password
                SALT,                   // salt bytes
                65536,                  // hash iterations
                256                     // key length in bits
        );

        // Generate the raw key bytes
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        // Wrap key bytes into an AES SecretKey object
        return new SecretKeySpec(keyBytes, "AES");
    }

    // -------------------------------------------------------
    // encrypt() — hides your message using password
    // Input:  plain text message + password
    // Output: encrypted text (unreadable string)
    // -------------------------------------------------------
    public static String encrypt(String message, String password) throws Exception {
        // Step 1: Generate AES key from password
        SecretKey key = generateKey(password);

        // Step 2: Create AES cipher in CBC mode with PKCS5 padding
        // AES/CBC/PKCS5Padding is standard and works on all JDK versions
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Step 3: Create IV specification object
        IvParameterSpec ivSpec = new IvParameterSpec(IV);

        // Step 4: Initialize cipher in ENCRYPT mode
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        // Step 5: Encrypt the message bytes
        byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

        // Step 6: Convert encrypted bytes to Base64 string
        // Base64 makes binary data safe to store as text
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // -------------------------------------------------------
    // decrypt() — reveals hidden message using password
    // Input:  encrypted text + same password used to encrypt
    // Output: original plain text message
    // -------------------------------------------------------
    public static String decrypt(String encryptedMessage, String password) throws Exception {
        // Step 1: Generate same AES key from same password
        SecretKey key = generateKey(password);

        // Step 2: Create same AES cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Step 3: Create same IV specification
        IvParameterSpec ivSpec = new IvParameterSpec(IV);

        // Step 4: Initialize cipher in DECRYPT mode this time
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        // Step 5: Decode Base64 string back to encrypted bytes
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);

        // Step 6: Decrypt bytes back to original message bytes
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // Step 7: Convert bytes back to readable string
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
