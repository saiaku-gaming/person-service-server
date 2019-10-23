package com.valhallagame.personserviceserver.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class SteamClient {

    private static final Logger logger = LoggerFactory.getLogger(SteamClient.class);
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    @Value("${steam.api.key}")
    String steamApiKey;

    @Value("${steam.application.id}")
    String steamAppId;

    public SteamClient() {
        client = new OkHttpClient();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    public Optional<String> checkAuthSession(String authSessionTicket) throws IOException {
        if (StringUtils.isBlank(steamApiKey) || StringUtils.isBlank(steamAppId)) {
            logger.error("steamApiKey or steamAppId was blank!");
            return Optional.empty();
        }
        String url = String.format("https://api.steampowered.com/ISteamUserAuth/AuthenticateUserTicket/v1/?key=%s&appid=%s&ticket=%s", steamApiKey, steamAppId, authSessionTicket);
        logger.info("Sending steam api request {}", url);
        okhttp3.Response response = get(url);
        if (response.code() != 200) {
            logger.error("error response from steam api: " + response.toString() + " with body" + response.body());
            return Optional.empty();
        }
        try {
            String body = response.body() == null ? null : response.body().string();
            logger.info("Steam responded to authTicket {} with body {}", authSessionTicket, body);
            AuthResponseWrapper authResponseWrapper = objectMapper.readValue(body, AuthResponseWrapper.class);
            if (authResponseWrapper.response.error != null) {
                return Optional.empty();
            }
            return Optional.of(authResponseWrapper.response.params.steamid);
        } catch (Exception e) {
            logger.error("Got error when parsing response body", e);
            return Optional.empty();
        }
    }

    private okhttp3.Response get(String url) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (MDC.getMDCAdapter() != null && MDC.get("request_id") != null) {
            builder.addHeader("X-REQUEST-ID", MDC.get("request_id"));
        }
        Request request = builder.get().build();
        return client.newCall(request).execute();
    }

    @SuppressWarnings("WeakerAccess")
    private static class AuthResponseWrapper {
        public Response response;
    }

    @SuppressWarnings("WeakerAccess")
    private static class Response {
        public Params params;
        public Error error;
    }

    @SuppressWarnings("WeakerAccess")
    private static class Params {
        public String steamid;
    }

    @SuppressWarnings({"unused"})
    private static class Error {
        public int errorcode;
        public String errordesc;
    }
}
