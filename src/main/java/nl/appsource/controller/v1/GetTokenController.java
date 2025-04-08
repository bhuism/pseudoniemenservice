package nl.appsource.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.appsource.pseudoniemenservice.generated.server.api.GetTokenApi;
import nl.appsource.pseudoniemenservice.generated.server.model.WsGetTokenRequest;
import nl.appsource.pseudoniemenservice.generated.server.model.WsGetTokenResponse;
import nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifier;
import nl.appsource.service.GetTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public final class GetTokenController implements GetTokenApi, VersionOneController {

    private final ObjectMapper objectMapper;

    private final GetTokenService getTokenService;

    /**
     * Retrieves a token based on the provided caller identifier and request details.
     *
     * @param wsGetTokenRequest The request object containing the recipient organization identifier
     *                          and additional details.
     * @return A ResponseEntity containing the token if the request is successful, or a
     * ResponseEntity with a status of UNPROCESSABLE_ENTITY if the token cannot be retrieved.
     */
    @Override
    public ResponseEntity<WsGetTokenResponse> getToken(final WsGetTokenRequest wsGetTokenRequest) {
        try {
            final String sender = wsGetTokenRequest.getSender();

            final WsIdentifier identifier = wsGetTokenRequest.getIdentifier();

            final StringWriter stringWriter = new StringWriter();
            objectMapper.writeValue(stringWriter, wsGetTokenRequest.getScope());
            final String scope = stringWriter.toString();

            log.info("getToken() sender={}, identifier={}, scope={}", sender, identifier, scope);

            final WsGetTokenResponse wsGetTokenResponse = getTokenService.getToken(sender, identifier, (Map<String, Object>) wsGetTokenRequest.getScope());
            return ResponseEntity.ok(wsGetTokenResponse);
        } catch (final Exception e) {
            log.error("", e);
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
