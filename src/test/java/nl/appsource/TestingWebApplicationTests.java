package nl.appsource;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestingWebApplicationTests {

    @Autowired
    private Environment environment;
    @Autowired
    private TestRestTemplate restTemplate;
    @LocalManagementPort
    private int managementPort;

    @Test
    public void actuatorHealthShouldReturnDefaultMessage() {
        assertThat(this.restTemplate.getForObject("http://localhost:" + managementPort + "/manage/health",
            String.class)).isEqualTo("{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}");
    }

    @Test
    public void actuatorHealthLiveNessShouldReturnDefaultMessage() {
        assertThat(this.restTemplate.getForObject("http://localhost:" + managementPort + "/manage/health/liveness",
            String.class)).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    public void actuatorHealthReadinessShouldReturnDefaultMessage() {
        assertThat(this.restTemplate.getForObject("http://localhost:" + managementPort + "/manage/health/readiness",
            String.class)).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    @DisplayName("""
        Given a request to get a token with a BSN identifier
        When sending the request to /v1/getToken
        Then the response should include a token
        And the token can be used to exchange for the identifier type BSN
        """)
    void testGetAtokenExchangeForBSN() {
        // get a token
        final Map<String, Object> getTokenBody = Map.of("sender", "00000008855800191020", "receiver", "00000000123450112345", "identifier",
            Map.of("type", "BSN", "value", "012345679"), "scope", Map.of("a", "b"));
        final HttpEntity<Map<String, Object>> httpEntityGetToken = new HttpEntity<>(getTokenBody);
        final ResponseEntity<Map> tokenExchange = restTemplate.exchange("/api/v1/getToken", HttpMethod.POST,
            httpEntityGetToken, Map.class);
        assertThat(tokenExchange.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tokenExchange)
            .extracting("body")
            .asInstanceOf(InstanceOfAssertFactories.map(String.class, Void.class))
            .containsKey("token");

        // change token for identifier
        final String token = (String) tokenExchange.getBody().get("token");
        final Map<String, Object> exchangeTokenBody = Map.of("token", token, "identifierType", "BSN", "organisation", "00000008855800191020", "scope", Map.of("a", "b"));
        final HttpEntity<Map<String, Object>> httpEntityExchangeToken = new HttpEntity<>(exchangeTokenBody);
        final ResponseEntity<Map> identifierExchange = restTemplate.exchange("/api/v1/exchangeToken", HttpMethod.POST,
            httpEntityExchangeToken,
            Map.class);
        assertThat(identifierExchange.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(identifierExchange)
            .extracting("body")
            .asInstanceOf(InstanceOfAssertFactories.map(String.class, Map.class))
            .containsExactly(entry("identifier", Map.of("type", "BSN", "value", "012345679")));
    }
}
