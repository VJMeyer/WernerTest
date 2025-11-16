package com.auth.keycloak.controller;

import com.auth.keycloak.dto.TokenInfo;
import com.auth.keycloak.dto.UserInfo;
import com.auth.keycloak.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserService userService;

    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        UserInfo userInfo = userService.getUserInfo(authentication);
        TokenInfo tokenInfo = userService.getTokenInfo(authentication);

        model.addAttribute("userInfo", userInfo);
        model.addAttribute("tokenInfo", tokenInfo);

        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        UserInfo userInfo = userService.getUserInfo(authentication);
        model.addAttribute("userInfo", userInfo);

        return "profile";
    }
}
