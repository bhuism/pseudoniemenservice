package nl.appsource.service;

import lombok.RequiredArgsConstructor;
import nl.appsource.model.v1.Identifier;
import nl.appsource.model.v1.Token;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeTokenRequest;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeTokenResponse;
import nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifier;
import nl.appsource.service.crypto.AesGcmCryptographerService;
import nl.appsource.service.crypto.AesGcmSivCryptographerService;
import nl.appsource.service.serializer.TokenSerializer;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import static nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes.BSN;
import static nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes.ORGANISATION_PSEUDO;

@Service
@RequiredArgsConstructor
public class ExchangeTokenService {

    private final AesGcmCryptographerService aesGcmCryptographerService;

    private final TokenSerializer tokenSerializer;

    private final AesGcmSivCryptographerService aesGcmSivCryptographerService;

    /**
     * exchange a token.
     * @param wsExchangeTokenForIdentifierRequest
     * @return the response
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidCipherTextException
     * @throws IOException
     */

    public ResponseEntity<WsExchangeTokenResponse> exchangeToken(final WsExchangeTokenRequest wsExchangeTokenForIdentifierRequest)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidCipherTextException, IOException {
        // lookup caller
        // final Organisation organisation = organisatieRepository.findByOin(callerOIN).orElseThrow(RuntimeException::new);

        // caller authorisation

        // decrypt token

        final String serializedToken = aesGcmCryptographerService.decrypt(wsExchangeTokenForIdentifierRequest.getToken(), wsExchangeTokenForIdentifierRequest.getOrganisation());

        // deserialize token

        final Token token = tokenSerializer.deSerialize(serializedToken);

        // validate token

        if (!Objects.equals(wsExchangeTokenForIdentifierRequest.getOrganisation(), token.getRecipientOIN())) {
            throw new RuntimeException("org-token mismatch");
        }

        // create response

        final WsExchangeTokenResponse.WsExchangeTokenResponseBuilder wsExchangeTokenResponseBuilder = WsExchangeTokenResponse.builder();

        final WsIdentifier.WsIdentifierBuilder wsIdentifierBuilder = WsIdentifier.builder();

        switch (wsExchangeTokenForIdentifierRequest.getIdentifierType()) {

            // no conversion
            case BSN:
                wsIdentifierBuilder.type(BSN).value(token.getBsn());
                break;

            // BSN -> ORHANISATION_PSEUDO conversion
            case ORGANISATION_PSEUDO:
                final Identifier identifier = Identifier.fromBsn(token.getBsn(), token.getScope());
                final String encryptedIdentifier = aesGcmSivCryptographerService.encryptIdentifier(identifier, wsExchangeTokenForIdentifierRequest.getOrganisation());
                wsIdentifierBuilder.type(ORGANISATION_PSEUDO).value(encryptedIdentifier);
                break;

            default:
                throw new RuntimeException("Unknown identifier type");

        }

        wsExchangeTokenResponseBuilder.identifier(wsIdentifierBuilder.build());

        return ResponseEntity.ok(wsExchangeTokenResponseBuilder.build());
    }


}
