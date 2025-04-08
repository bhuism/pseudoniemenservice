package nl.appsource.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nl.appsource.model.v1.Identifier;
import nl.appsource.model.v1.Token;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeTokenResponse;
import nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifier;
import nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes;
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
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;

import static nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes.BSN;
import static nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes.ORGANISATION_PSEUDO;

@Service
@RequiredArgsConstructor
public class ExchangeTokenService {

    private final AesGcmCryptographerService aesGcmCryptographerService;

    private final TokenSerializer tokenSerializer;

    private final AesGcmSivCryptographerService aesGcmSivCryptographerService;

    private final ObjectMapper objectMapper;

    /**
     * Exchange a token for an Identifier.
     * @param tokenString
     * @param identifierType
     * @param scope
     * @param organisation
     * @return the identifier.
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidCipherTextException
     * @throws IOException
     */

    public ResponseEntity<WsExchangeTokenResponse> exchangeToken(final String tokenString, final WsIdentifierTypes identifierType, final Map<String, Object> scope, final String organisation)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidCipherTextException, IOException {
        // lookup caller
        // final Organisation organisation = organisatieRepository.findByOin(callerOIN).orElseThrow(RuntimeException::new);

        // caller authorisation

        // decrypt token

        final String serializedToken = aesGcmCryptographerService.decrypt(tokenString, organisation);

        // deserialize token

        final Token token = tokenSerializer.deSerialize(serializedToken);

        // validate token

        if (!Objects.equals(organisation, token.getRecipientOIN())) {
            throw new RuntimeException("org-token mismatch");
        }

        final StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, scope);
        final String parameterScope = stringWriter.toString();

        final StringWriter stringWriter2 = new StringWriter();
        objectMapper.writeValue(stringWriter2, token.getScope());
        final String tokenScope = stringWriter2.toString();


        if (!Objects.equals(parameterScope, tokenScope)) {
            throw new RuntimeException("scope mismatch");
        }

        // create response

        final WsExchangeTokenResponse.WsExchangeTokenResponseBuilder wsExchangeTokenResponseBuilder = WsExchangeTokenResponse.builder();

        final WsIdentifier.WsIdentifierBuilder wsIdentifierBuilder = WsIdentifier.builder();

        switch (identifierType) {

            // no conversion
            case BSN:
                wsIdentifierBuilder.type(BSN).value(token.getBsn());
                break;

            // BSN -> ORHANISATION_PSEUDO conversion
            case ORGANISATION_PSEUDO:
                final Identifier identifier = Identifier.fromBsn(token.getBsn(), token.getScope());
                final String encryptedIdentifier = aesGcmSivCryptographerService.encryptIdentifier(identifier, organisation);
                wsIdentifierBuilder.type(ORGANISATION_PSEUDO).value(encryptedIdentifier);
                break;

            default:
                throw new RuntimeException("Unknown identifier type");

        }

        wsExchangeTokenResponseBuilder.identifier(wsIdentifierBuilder.build());

        return ResponseEntity.ok(wsExchangeTokenResponseBuilder.build());
    }


}
