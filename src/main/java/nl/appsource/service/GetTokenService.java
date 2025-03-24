package nl.appsource.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.appsource.model.v1.Token;
import nl.appsource.pseudoniemenservice.generated.server.model.WsGetTokenResponse;
import nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifier;
import nl.appsource.service.crypto.AesGcmCryptographerService;
import nl.appsource.service.crypto.AesGcmSivCryptographerService;
import nl.appsource.service.serializer.TokenSerializer;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public final class GetTokenService {

    private final AesGcmSivCryptographerService aesGcmSivCryptographerService;
    private final AesGcmCryptographerService aesGcmCryptographerService;
    private final TokenSerializer tokenSerializer;

    public WsGetTokenResponse getWsGetTokenResponse(final String recipientOIN,
                                                    final WsIdentifier identifier,
                                                    final String scope)
        throws InvalidCipherTextException, IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        final long creationDate = System.currentTimeMillis();

        final String identifierValue = identifier.getValue();

        final String bsn;

        switch (identifier.getType()) {
            case BSN:
                bsn = identifierValue;
                break;
            case ORGANISATION_PSEUDO:
                bsn = aesGcmSivCryptographerService.decryptIdentifier(identifierValue, recipientOIN).getBsn();
                break;
            default:
                throw new IllegalArgumentException(
                    "Unsupported identifier type: " + identifier.getType());
        }

        final Token token = Token.fromBsn(bsn, recipientOIN, creationDate, scope);

        final String encryptedTokenString = aesGcmCryptographerService.encryptToken(token, recipientOIN);

        return WsGetTokenResponse.builder()
            .token(encryptedTokenString)
            .build();

    }
}
