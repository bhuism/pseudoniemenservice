package nl.appsource.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.appsource.configuration.PseudoniemenServiceProperties;
import nl.appsource.model.v1.Identifier;
import nl.appsource.service.crypto.AesGcmSivCryptographerService;
import nl.appsource.service.crypto.AesGcmSivCryptographerServiceImpl;
import nl.appsource.service.serializer.IdentifierSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Class for testing {@link AesGcmSivCryptographerService}
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ObjectMapper.class, AesGcmSivCryptographerServiceImpl.class, IdentifierSerializer.class, TestAesGcmSivCryptographerServiceImpl.TestConfiguration.class})
class TestAesGcmSivCryptographerServiceImpl {

    @Autowired
    private AesGcmSivCryptographerService aesGcmSivCryptographerService;

    private final Set<String> testStrings = new HashSet<>(Arrays.asList("a", "bb", "dsv", "ghad", "dhaht", "uDg5Av", "d93fdvv", "dj83hzHo", "38iKawKv9", "dk(gkzm)Mh", "gjk)s3$g9cQ"));

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfiguration {
        @Bean
        public PseudoniemenServiceProperties pseudoniemenServiceProperties() {
            return new PseudoniemenServiceProperties()
                .setTokenPrivateKey("i4dfBykN5Fjw9p3ADxvpRUhpbFSXepRSOcRGuaiJ4iQ=")
                .setIdentifierPrivateKey("b2RPRGh6aThiMmluVEpMWVVJM2lOTGlWekVCU2hDMEU=");
        }
    }

    @Test
    @DisplayName("""
        Given a set of test strings
        When encrypting and decrypting each string with a specific key
        Then the decrypted identifier's BSN should be equal to the original plain string
        """)
    void testEncyptDecryptIdentifierForDifferentStringLengths() {

        testStrings.forEach(plain -> {
            try {
                // GIVEN
                final String crypted = aesGcmSivCryptographerService.encryptIdentifier(Identifier.fromBsn(plain, null), "helloHowAreyo12345678");
                // WHEN
                final Identifier actual = aesGcmSivCryptographerService.decryptIdentifier(crypted, "helloHowAreyo12345678");
                // THEN
                assertThat(actual.getBsn()).isEqualTo(plain);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("""
        Given the same plaintext message and encryption key
        When encrypting the message twice
        Then the resulting ciphertexts should be the same due to SIV mode
        """)
    @SneakyThrows
    void testCiphertextIsTheSameForSamePlaintext() {
        // GIVEN
        final String plaintext = "This is a test message to ensure ciphertext is different!";
        final Identifier identifier = Identifier.fromBsn(plaintext, null);
        // WHEN
        final String encryptedMessage1 = aesGcmSivCryptographerService.encryptIdentifier(identifier, "aniceSaltGorYu");
        final String encryptedMessage2 = aesGcmSivCryptographerService.encryptIdentifier(identifier, "aniceSaltGorYu");
        // THEN
        // Assert that the two ciphertexts are the same
        assertThat(encryptedMessage1).isEqualTo(encryptedMessage2);
    }
}
