package de.kisters.hmt.cloud.services.upload.controller.web;

import de.kisters.hmt.cloud.services.upload.security.JwtUserInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Main web controller for serving HTML pages.
 * Uses Thymeleaf for server-side rendering.
 */
@Controller
public class WebController {

    /**
     * Home page - redirects to dashboard if authenticated, login otherwise.
     */
    @GetMapping("/")
    public String index() {
        JwtUserInfo userInfo = JwtUserInfo.fromSecurityContext();
        if (userInfo != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    /**
     * Login page.
     */
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("keycloakUrl", "http://localhost:8180");
        model.addAttribute("realm", "file-processing");
        model.addAttribute("clientId", "upload-web-app");
        return "login";
    }

    /**
     * Main dashboard - file upload and monitoring.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        JwtUserInfo userInfo = JwtUserInfo.fromSecurityContext();

        if (userInfo == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", userInfo);
        model.addAttribute("username", userInfo.getUsername());
        model.addAttribute("email", userInfo.getEmail());
        model.addAttribute("organization", userInfo.getOrganization());
        model.addAttribute("isAdmin", userInfo.isAdmin());
        model.addAttribute("roles", userInfo.getRoles());

        return "dashboard";
    }

    /**
     * Admin dashboard - view all uploads for organization.
     */
    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        JwtUserInfo userInfo = JwtUserInfo.fromSecurityContext();

        if (userInfo == null) {
            return "redirect:/login";
        }

        if (!userInfo.isAdmin()) {
            return "redirect:/dashboard";
        }

        model.addAttribute("user", userInfo);
        model.addAttribute("username", userInfo.getUsername());
        model.addAttribute("organization", userInfo.getOrganization());
        model.addAttribute("orgId", userInfo.getOrgId());

        return "admin";
    }

    /**
     * User profile page.
     */
    @GetMapping("/profile")
    public String profile(Model model) {
        JwtUserInfo userInfo = JwtUserInfo.fromSecurityContext();

        if (userInfo == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", userInfo);
        model.addAttribute("username", userInfo.getUsername());
        model.addAttribute("email", userInfo.getEmail());
        model.addAttribute("organization", userInfo.getOrganization());
        model.addAttribute("roles", userInfo.getRoles());
        return "profile";
    }

    /**
     * Logout page - displays a page that clears JWT token and redirects to login.
     * Since we use stateless JWT authentication, logout is handled client-side.
     */
    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }
}
