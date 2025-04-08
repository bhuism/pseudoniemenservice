package nl.appsource.service.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.appsource.configuration.PseudoniemenServiceProperties;
import nl.appsource.model.v1.Token;
import nl.appsource.service.serializer.TokenSerializer;
import nl.appsource.utils.AesUtil;
import nl.appsource.utils.Base64Util;
import nl.appsource.utils.ByteArrayUtil;
import nl.appsource.utils.MessageDigestFactory;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static nl.appsource.utils.AesUtil.IV_LENGTH;

/**
 * Advanced Encryption Standard  Galois/Counter Mode (AES-GCM).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AesGcmCryptographerServiceImpl implements AesGcmCryptographerService {

    private final PseudoniemenServiceProperties pseudoniemenServiceProperties;

    private final TokenSerializer tokenSerializer;

    /**
     * Encrypts the given plaintext using the Advanced Encryption Standard in Galois/Counter Mode
     * (AES-GCM) and a provided salt. The resulting ciphertext is Base64 encoded and includes the IV
     * used during encryption, concatenated with the encrypted data.
     *
     * @param salt the salt value used to derive the encryption key
     * @return the Base64 encoded ciphertext, including the IV
     * @throws IllegalBlockSizeException          if the block size is invalid during the encryption
     *                                            process
     * @throws BadPaddingException                if there are issues with padding during
     *                                            encryption
     * @throws InvalidAlgorithmParameterException if the provided algorithm parameters are invalid
     * @throws InvalidKeyException                if the encryption key is invalid
     * @throws NoSuchAlgorithmException           if the requested encryption algorithm is not
     *                                            available
     * @throws NoSuchPaddingException             if the requested padding scheme is not available
     */
    @Override
    public String encryptToken(final Token token, final String salt) throws IllegalBlockSizeException,
        BadPaddingException,
        InvalidAlgorithmParameterException,
        InvalidKeyException,
        NoSuchAlgorithmException,
        NoSuchPaddingException, IOException {

        final String plaintext = tokenSerializer.serialize(token);

        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be null or empty");
        }

        return encrypt(plaintext, salt);
    }

    /**
     * Encrypt a string.
     * @param plaintext text to encrypt
     * @param salt to use in the encryption
     * @return encrypted string
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     */

    public String encrypt(final String plaintext, final String salt) throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {

        final Cipher cipher = AesUtil.createCipher();
        final GCMParameterSpec gcmParameterSpec = AesUtil.generateIV();
        final SecretKey secretKey = createSecretKey(salt);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        final byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        final byte[] gcmIV = gcmParameterSpec.getIV();
        final byte[] encryptedWithIV = ByteArrayUtil.concat(gcmIV, ciphertext);
        return Base64Util.encodeToString(encryptedWithIV);
    }

    /**
     * Creates a secret encryption key by combining a base64-decoded private key with a given salt,
     * and hashing the result using SHA-256.
     *
     * @param salt the salt value used to modify the private key and derive the final encryption
     *             key
     * @return a SecretKey instance derived from the combined and hashed input
     */
    @Override
    public SecretKey createSecretKey(final String salt) {

        final byte[] keyBytes = Base64Util.decode(
            pseudoniemenServiceProperties.getTokenPrivateKey());
        final byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
        final byte[] salterSecretBytes = ByteArrayUtil.concat(keyBytes, saltBytes);
        final byte[] key = MessageDigestFactory.instance().digest(salterSecretBytes);
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Decrypts the given Base64 encoded ciphertext, which includes the initialization vector (IV),
     * using the Advanced Encryption Standard in Galois/Counter Mode (AES-GCM) with a provided
     * salt.
     *
     * @param ciphertextWithIv the Base64 encoded encrypted data, including the IV
     * @param salt             the salt value used to derive the decryption key
     * @return the decrypted plaintext as a UTF-8 string
     * @throws NoSuchPaddingException             if the requested padding scheme is not available
     * @throws NoSuchAlgorithmException           if the requested encryption algorithm is not
     *                                            available
     * @throws InvalidAlgorithmParameterException if the provided algorithm parameters are invalid
     * @throws InvalidKeyException                if the decryption key is invalid
     * @throws IllegalBlockSizeException          if the block size is invalid during the decryption
     *                                            process
     * @throws BadPaddingException                if there are issues with padding during
     *                                            decryption
     */
    @Override
    public String decrypt(final String ciphertextWithIv, final String salt)
        throws NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidAlgorithmParameterException,
        InvalidKeyException,
        IllegalBlockSizeException,
        BadPaddingException {

        final Cipher cipher = AesUtil.createCipher();
        final byte[] encryptedWithIV = Base64Util.decode(ciphertextWithIv);
        final byte[] iv = Arrays.copyOfRange(encryptedWithIV, 0, IV_LENGTH);
        final byte[] ciphertext = Arrays.copyOfRange(encryptedWithIV, IV_LENGTH,
            encryptedWithIV.length);
        final GCMParameterSpec gcmParameterSpec = AesUtil.createIVfromValues(iv);
        final SecretKey secretKey = createSecretKey(salt);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        final byte[] decryptedText = cipher.doFinal(ciphertext);
        return new String(decryptedText, StandardCharsets.UTF_8);
    }
}
