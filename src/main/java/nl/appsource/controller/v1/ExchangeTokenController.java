package nl.appsource.controller.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.appsource.pseudoniemenservice.generated.server.api.ExchangeTokenApi;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeTokenRequest;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeTokenResponse;
import nl.appsource.service.ExchangeTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public final class ExchangeTokenController implements ExchangeTokenApi, VersionOneController {

    private final ExchangeTokenService exchangeTokenService;

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

            return exchangeTokenService.exchangeToken(wsExchangeTokenForIdentifierRequest);

        } catch (final Exception e) {
            log.error("", e);
            return ResponseEntity.unprocessableEntity().build();
        }
    }


}
