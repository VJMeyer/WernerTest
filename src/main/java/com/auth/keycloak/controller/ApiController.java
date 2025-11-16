package com.auth.keycloak.controller;

import com.auth.keycloak.dto.TokenInfo;
import com.auth.keycloak.dto.UserInfo;
import com.auth.keycloak.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class ApiController {

    private final UserService userService;

    public ApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String getProfileFragment(Authentication authentication, Model model) {
        UserInfo userInfo = userService.getUserInfo(authentication);
        model.addAttribute("userInfo", userInfo);
        return "fragments/profile-card :: profile-card";
    }

    @GetMapping("/token-info")
    public String getTokenInfoFragment(Authentication authentication, Model model) {
        TokenInfo tokenInfo = userService.getTokenInfo(authentication);
        model.addAttribute("tokenInfo", tokenInfo);
        return "fragments/token-info :: token-info";
    }
}
