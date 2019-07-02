package com.valhallagame.personserviceserver.client;

import com.valhallagame.common.RestCaller;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class SteamClient extends RestCaller {

    @Value("${steam.api.key}")
    String steamApiKey;

    @Value("${steam.application.id}")
    String steamAppId;

    public Optional<String> checkAuthSession(String authSessionTicket) throws IOException {
        if (StringUtils.isBlank(steamApiKey) || StringUtils.isBlank(steamAppId)) {
            return Optional.empty();
        }
        String url = String.format("https://api.steampowered.com/ISteamUserAuth/AuthenticateUserTicket/v1/?key=%s&appid=%s&ticket=%s", steamApiKey, steamAppId, authSessionTicket);
        Optional<AuthResponseWrapper> authResponseWrapperOpt = getCall(url, AuthResponseWrapper.class).get();
        return authResponseWrapperOpt.flatMap(authResponseWrapper -> {
            if (authResponseWrapper.response == null || authResponseWrapper.response.params == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(authResponseWrapper.response.params.steamid);
        });
    }

    @SuppressWarnings("WeakerAccess")
    private static class AuthResponseWrapper {
        public Response response;
    }

    @SuppressWarnings("WeakerAccess")
    private static class Response {
        public Params params;
    }

    @SuppressWarnings("WeakerAccess")
    private static class Params {
        public String steamid;
    }
}
