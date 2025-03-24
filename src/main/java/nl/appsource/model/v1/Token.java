package nl.appsource.model.v1;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class Token {

    private static final String VERSION = "V1";

    private String version;
    private String bsn;
    private String recipientOIN;
    private Long creationDate;
    private String scope;

    public static Token fromBsn(final String bsn, final String oin, final Long creationDate, final String scope) {

        final Token token = new Token();

        token.setVersion(VERSION);
        token.setBsn(bsn);
        token.setRecipientOIN(oin);
        token.setCreationDate(creationDate);
        token.setScope(scope);

        return token;
    }

}
