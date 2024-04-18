package com.edw.springkeycloak.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.WebUtils;

@Component
@Log4j2
public class KeycloakLogoutHandler implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakLogoutHandler.class);
    private final RestTemplate restTemplate;
    private boolean invalidateHttpSession = false;
    private boolean clearAuthentication = true;
    public static final String MOA_SERVICE_SET_ID = "MSSID";
    public static final String SIGN_IN_USER = "sign_in_user";
    public static final String AUTH_TOKEN = "moa-auth-token";

    @Setter
    private HashOperations<String, String, String> hashOperations;


    public KeycloakLogoutHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        log.info("test");
        logoutFromKeycloak((OidcUser) authentication.getPrincipal());
        Assert.notNull(request, "HttpServlet required");
        if(invalidateHttpSession) {
            HttpSession session = request.getSession(false);
            if(session != null){
                log.debug("Invalidating session: {} ",session.getId());
                session.invalidate();
            }
        }
        if(clearAuthentication) {
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(null);
        }
        SecurityContextHolder.clearContext();
        clearRedisSessionUserInfo(request);
        handleLogOutResponse(request, response);

    }


    private void logoutFromKeycloak(OidcUser user) {
        log.info("user {}", user.getIdToken().getTokenValue());
        String endSessionEndpoint = user.getIssuer() + "/protocol/openid-connect/logout";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(endSessionEndpoint)
                .queryParam("id_token_hint", user.getIdToken().getTokenValue());

        ResponseEntity<String> logoutResponse = restTemplate.getForEntity(builder.toUriString(), String.class);
        if (logoutResponse.getStatusCode().is2xxSuccessful()) {
            logger.info("Successfully logged out from Keycloak");
        } else {
            logger.error("Could not propagate logout to Keycloak");
        }
    }

    private void clearRedisSessionUserInfo(HttpServletRequest request) {
        Cookie ck = WebUtils.getCookie(request, MOA_SERVICE_SET_ID);
        if(ck != null) {
            hashOperations.delete(ck.getValue(), SIGN_IN_USER, AUTH_TOKEN);
        }
    }


    /**
     * This method would edit the cookie information and make MSUS empty
     * while responding to logout. This would further help in order to. This would help
     * to avoid same cookie ID each time a person logs in
     * @param response
     */
    private void handleLogOutResponse(HttpServletRequest request, HttpServletResponse response) {
        Cookie mssid = WebUtils.getCookie(request, MOA_SERVICE_SET_ID);
        if(mssid != null) {
            mssid.setMaxAge(0);
            mssid.setValue(null);
            mssid.setPath("/");
            mssid.setDomain("miracleofasia.com");
            response.addCookie(mssid);
        }
    }

}