package nl.appsource.controller.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.appsource.model.v1.Identifier;
import nl.appsource.model.v1.Token;
import nl.appsource.persistence.OrganisatieRepository;
import nl.appsource.pseudoniemenservice.generated.server.api.ExchangeTokenApi;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeTokenRequest;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeTokenResponse;
import nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifier;
import nl.appsource.service.crypto.AesGcmCryptographerService;
import nl.appsource.service.crypto.AesGcmSivCryptographerService;
import nl.appsource.service.serializer.TokenSerializer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes.BSN;
import static nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes.ORGANISATION_PSEUDO;

@Slf4j
@RequiredArgsConstructor
@RestController
public final class ExchangeTokenController implements ExchangeTokenApi, VersionOneController {

    private final AesGcmCryptographerService aesGcmCryptographerService;

    private final TokenSerializer tokenSerializer;

    private final AesGcmSivCryptographerService aesGcmSivCryptographerService;

    private final OrganisatieRepository organisatieRepository;

    /**
     * Handles the exchange of a token and returns the corresponding identifier in a response. This
     * method validates the caller's OIN, processes the incoming token using the specified
     * identifier type, and constructs a response accordingly.
     *
     * @param wsExchangeTokenForIdentifierRequest The request containing the token and identifier
     *                                            type details.
     * @return A response entity containing the converted identifier or a status indicating failure.
     */
    @Override
    public ResponseEntity<WsExchangeTokenResponse> exchangeToken(final WsExchangeTokenRequest wsExchangeTokenForIdentifierRequest) {
        try {

            // lookup caller
            // final Organisation organisation = organisatieRepository.findByOin(callerOIN).orElseThrow(RuntimeException::new);

            // caller authorisation

            // decrypt token

            final String serializedToken = aesGcmCryptographerService.decrypt(wsExchangeTokenForIdentifierRequest.getToken(), wsExchangeTokenForIdentifierRequest.getOrganisation());

            // deserialize token

            final Token token = tokenSerializer.deSerialize(serializedToken);

            // validate token

            if (!Objects.equals(wsExchangeTokenForIdentifierRequest.getOrganisation(), token.getRecipientOIN())) {
                throw new RuntimeException("CallerOIN and token mismatch");
            }

            // create response

            final WsExchangeTokenResponse.WsExchangeTokenResponseBuilder wsExchangeTokenResponseBuilder = WsExchangeTokenResponse.builder();

            final WsIdentifier.WsIdentifierBuilder wsIdentifierBuilder = WsIdentifier.builder();

            switch (wsExchangeTokenForIdentifierRequest.getIdentifierType()) {

                // no convesion
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

        } catch (final Exception e) {
            log.error("", e);
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
