package api;

import config.Config;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import static io.restassured.RestAssured.given;

@Slf4j
public abstract class BaseApi {

    protected abstract String getBasePath();

    public enum AuthType {
        ADMIN,
        NONE,
        CUSTOM
    }

    protected RequestSpecification getRequest(AuthType authType, String... credentials) {
        RequestSpecification spec = given()
                .baseUri(Config.getBaseUrl())
                .basePath(getBasePath())
                .contentType("application/json")
                .accept("application/json");

        switch (authType) {
            case ADMIN:
                spec = spec.auth().preemptive().basic(Config.getApiUser(), Config.getApiPassword());
                break;
            case CUSTOM:
                if (credentials.length >= 2) {
                    spec = spec.auth().preemptive().basic(credentials[0], credentials[1]);
                }
                break;
            case NONE:
            default:
                break;
        }

        return spec;
    }
}