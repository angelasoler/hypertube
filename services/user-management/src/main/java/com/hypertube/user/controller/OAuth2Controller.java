package com.hypertube.user.controller;

import com.hypertube.user.dto.AuthResponse;
import com.hypertube.user.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for OAuth2 authentication callbacks
 * Handles success/failure redirects for 42, Google, and GitHub
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * OAuth2 login success callback for 42 school
     */
    @GetMapping("/callback/42")
    public RedirectView oauth42Callback(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            AuthResponse response = oauth2Service.processOAuth2Login(oauth2User, "42");
            log.info("42 OAuth login successful");

            // Redirect to frontend with tokens in URL params (will be extracted by frontend)
            return new RedirectView(String.format(
                "%s/oauth/callback?access_token=%s&refresh_token=%s&expires_in=%d",
                frontendUrl,
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getExpiresIn()
            ));
        } catch (Exception e) {
            log.error("42 OAuth login failed", e);
            return new RedirectView(frontendUrl + "/login?error=oauth_failed");
        }
    }

    /**
     * OAuth2 login success callback for Google
     */
    @GetMapping("/callback/google")
    public RedirectView oauthGoogleCallback(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            AuthResponse response = oauth2Service.processOAuth2Login(oauth2User, "google");
            log.info("Google OAuth login successful");

            return new RedirectView(String.format(
                "%s/oauth/callback?access_token=%s&refresh_token=%s&expires_in=%d",
                frontendUrl,
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getExpiresIn()
            ));
        } catch (Exception e) {
            log.error("Google OAuth login failed", e);
            return new RedirectView(frontendUrl + "/login?error=oauth_failed");
        }
    }

    /**
     * OAuth2 login success callback for GitHub
     */
    @GetMapping("/callback/github")
    public RedirectView oauthGithubCallback(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            AuthResponse response = oauth2Service.processOAuth2Login(oauth2User, "github");
            log.info("GitHub OAuth login successful");

            return new RedirectView(String.format(
                "%s/oauth/callback?access_token=%s&refresh_token=%s&expires_in=%d",
                frontendUrl,
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getExpiresIn()
            ));
        } catch (Exception e) {
            log.error("GitHub OAuth login failed", e);
            return new RedirectView(frontendUrl + "/login?error=oauth_failed");
        }
    }

    /**
     * OAuth2 login failure handler
     */
    @GetMapping("/callback/failure")
    public RedirectView oauthFailure() {
        log.warn("OAuth login failed");
        return new RedirectView(frontendUrl + "/login?error=oauth_failed");
    }
}
