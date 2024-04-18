
package com.edw.springkeycloak.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class IndexController {

    @GetMapping(path = "/login/keycloak")
    public String customers(OAuth2AuthenticationToken principal) {

        log.info("getting username", principal.getName());
        return "customers";
    }

    @GetMapping("/*")
    public String index(@RequestParam String id) {
        return "external";
    }
}