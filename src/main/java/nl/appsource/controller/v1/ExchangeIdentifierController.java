package nl.appsource.controller.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.appsource.persistence.OrganisatieRepository;
import nl.appsource.pseudoniemenservice.generated.server.api.ExchangeIdentifierApi;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeIdentifierRequest;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeIdentifierResponse;
import nl.appsource.service.ExchangeIdentifierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public final class ExchangeIdentifierController implements ExchangeIdentifierApi,
    VersionOneController {

    private final ExchangeIdentifierService exchangeIdentifierService;

    // private final OrganisatieRepository organisatieRepository;

    /**
     * Exchanges an identifier based on the provided caller OIN and request data.
     *
     * @param wsExchangeRequest The request object containing the identifier and additional data for
     *                          the exchange process.
     * @return A ResponseEntity containing a WsExchangeIdentifierResponse if the exchange is
     * successful, or a ResponseEntity with HTTP status UNPROCESSABLE_ENTITY if the exchange fails.
     */
    @Override
    public ResponseEntity<WsExchangeIdentifierResponse> exchangeIdentifier(final WsExchangeIdentifierRequest wsExchangeRequest) {
        try {

            // lookup caller
            // final Organisation organisation = organisatieRepository.findByOin(callerOIN).orElseThrow(RuntimeException::new);

            final WsExchangeIdentifierResponse identifier = exchangeIdentifierService.exchangeIdentifier(wsExchangeRequest);
            return ResponseEntity.ok(identifier);
        } catch (final Exception e) {
            log.error("", e);
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
