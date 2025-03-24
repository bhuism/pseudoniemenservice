package nl.appsource.service;

import lombok.RequiredArgsConstructor;
import nl.appsource.model.v1.Identifier;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeIdentifierRequest;
import nl.appsource.pseudoniemenservice.generated.server.model.WsExchangeIdentifierResponse;
import nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifier;
import nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes;
import nl.appsource.service.crypto.AesGcmSivCryptographerService;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes.BSN;
import static nl.appsource.pseudoniemenservice.generated.server.model.WsIdentifierTypes.ORGANISATION_PSEUDO;

@RequiredArgsConstructor
@Service
public final class ExchangeIdentifierService {

    private final AesGcmSivCryptographerService aesGcmSivCryptographerService;

    public WsExchangeIdentifierResponse exchangeIdentifier(final WsExchangeIdentifierRequest wsExchangeIdentifierForIdentifierRequest) throws InvalidCipherTextException, IOException {

        final WsIdentifier wsIdentifierRequest = wsExchangeIdentifierForIdentifierRequest.getIdentifier();
        final String organisation = wsExchangeIdentifierForIdentifierRequest.getOrganisation();
        final WsIdentifierTypes recipientIdentifierType = wsExchangeIdentifierForIdentifierRequest.getRecipientIdentifierType();

        final WsExchangeIdentifierResponse.WsExchangeIdentifierResponseBuilder wsExchangeIdentifierResponseBuilder = WsExchangeIdentifierResponse.builder();

        final WsIdentifier.WsIdentifierBuilder wsIdentifierBuilder = WsIdentifier.builder();

        if (BSN.equals(wsIdentifierRequest.getType()) && ORGANISATION_PSEUDO.equals(recipientIdentifierType)) {

            // BSN to ORG_PSEUDO
            final Identifier identifier = Identifier.fromBsn(wsIdentifierRequest.getValue(), wsExchangeIdentifierForIdentifierRequest.getScope());

            final String encryptedIdentifier = aesGcmSivCryptographerService.encryptIdentifier(identifier, organisation);

            wsIdentifierBuilder
                .type(ORGANISATION_PSEUDO)
                .value(encryptedIdentifier);

        } else if (ORGANISATION_PSEUDO.equals(wsIdentifierRequest.getType()) && BSN.equals(recipientIdentifierType)) {

            // ORG_PSEUDO to BSN
            final String bsn = aesGcmSivCryptographerService.decryptIdentifier(wsIdentifierRequest.getValue(), organisation).getBsn();

            wsIdentifierBuilder
                .type(BSN)
                .value(bsn);

        } else {
            throw new RuntimeException("Unsupported types for convertion");
        }

        wsExchangeIdentifierResponseBuilder.identifier(wsIdentifierBuilder.build());


        return wsExchangeIdentifierResponseBuilder.build();

    }
}
