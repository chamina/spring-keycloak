package com.edw.springkeycloak.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Log4j2
public class OidcAuthenticationSuccessHandler implements AuthenticationSuccessHandler {


    private static final String MOA_SIGN_IN_USER_SELECTOR = "MSUS";
    public static final String MOA_SERVICE_SET_ID = "MSSID";
    public  static final String SIGN_IN_USER = "sign_in_user";
    public static final String AUTH_TOKEN = "moa-auth-token";

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Setter
    private HashOperations<String, String, String> hashOperations;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String redirectUrl = "https://dev.miracleofasia.com";
        // Custom logic to handle successful OIDC authentication
        // You can access information from the authentication object

        // You can also get details specific to OAuth2
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String userSelector = oidcUser.getClaim("sub");
        String jwtId = oidcUser.getClaim("jti");
        String jwtToken =  oidcUser.getIdToken().getTokenValue();
        log.info("logging userselctor {}", userSelector);
        log.info("logging jwt id {}", jwtId);

        // Access OAuth 2.0 tokens
        log.info("token {}", oidcUser.getIdToken().getTokenValue());


        Cookie msus = new Cookie(MOA_SIGN_IN_USER_SELECTOR, userSelector);
        msus.setHttpOnly(true);
        msus.setSecure(true);
        msus.setPath("/");
        msus.setDomain("miracleofasia.com");
        msus.setMaxAge(-1);

        final String mssidToken = jwtId;
        Cookie mssid = new Cookie(MOA_SERVICE_SET_ID, mssidToken);
        mssid.setHttpOnly(true);
        mssid.setSecure(true);
        mssid.setPath("/");
        mssid.setDomain("miracleofasia.com");
        //mssid.setDomain(request.getServletContext().getInitParameter("cookie-domain"));
        mssid.setMaxAge(2592000);

        hashOperations.put(mssidToken, SIGN_IN_USER, userSelector);
        hashOperations.put(mssidToken, AUTH_TOKEN, jwtToken);
        response.addCookie(msus);
        response.addCookie(mssid);
        log.info("cookie {}", request.getServletContext().getInitParameter("cookie-domain"));
        //response.sendRedirect("https://dev.miracleofasia.com");
        // Redirect or perform other actions based on your requirements

        redirectStrategy.sendRedirect(request, response, redirectUrl);

    }

}
