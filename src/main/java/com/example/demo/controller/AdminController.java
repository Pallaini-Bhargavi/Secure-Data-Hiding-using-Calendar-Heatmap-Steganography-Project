package com.example.demo.controller;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Admin;
import com.example.demo.repository.AdminRepository;
import com.example.demo.repository.ResetPasswordRequestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AdminService;
import com.example.demo.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ResetPasswordRequestRepository resetRepo;

    @Autowired
    private MailService mailService;

    /* ================= ADMIN LOGIN PAGE ================= */
    @GetMapping("/admin-login")
    public String adminLoginPage() {
        return "admin-login";
    }

    /* ================= ADMIN LOGIN PROCESS ================= */
    @PostMapping("/admin-login")public String adminLogin(@RequestParam String email,
                         @RequestParam String password,
                         HttpSession session,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {

        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Admin email is required.");
            return "redirect:/admin-login";
        }

        if (password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Admin password is required.");
            return "redirect:/admin-login";
        }

        Admin admin = adminRepository.findByEmail(email).orElse(null);
        if (admin == null) {
            redirectAttributes.addFlashAttribute("error", "Admin not found.");
            return "redirect:/admin-login";
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

        // 🔒 LOCK CHECK
        if (admin.getLockedUntil() != null &&
            admin.getLockedUntil().isAfter(LocalDateTime.now())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Account locked until " + admin.getLockedUntil().format(formatter)
            );
            return "redirect:/admin-login";
        }

        // ❌ WRONG PASSWORD
       if (!adminService.login(email, password)) {

    int attempts = admin.getLoginAttempts() + 1;
    admin.setLoginAttempts(attempts);

    //  SEND MAIL FOR EVERY WRONG ATTEMPT
try {
    mailService.sendTextMail((
        admin.getEmail(),
        "⚠️ Security Alert: Suspicious Login Attempt",
        """
        We detected multiple failed login attempts on your admin account.
        
        If this was you, you can ignore this message.
        
        If you did not attempt to login, please ensure your credentials are secure.
        
        Time: """ + LocalDateTime.now() + "\n" +
        "IP Address: " + request.getRemoteAddr()
    );

} catch (Exception e) {
}

    // 🔒 LOCK AFTER 3 ATTEMPTS
    if (attempts >= 3) {
        admin.setLockedUntil(LocalDateTime.now().plusHours(48));
        adminRepository.save(admin);

        redirectAttributes.addFlashAttribute(
                "error",
                "Account locked for 48 hours due to 3 failed attempts."
        );
        return "redirect:/admin-login";
    }

    adminRepository.save(admin);

    redirectAttributes.addFlashAttribute(
            "error",
            "Invalid password. Attempt " + attempts + " / 3"
    );
    return "redirect:/admin-login";
}
        //  SUCCESS LOGIN
        admin.setLoginAttempts(0);
        admin.setLockedUntil(null);
        adminRepository.save(admin);

        session.setAttribute("ADMIN_EMAIL", admin.getEmail());

        redirectAttributes.addFlashAttribute("adminLoginSuccess", true);
        return "redirect:/admin-login";
    }

    /* ================= ADMIN DASHBOARD ================= */
    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session, Model model) {

        if (session.getAttribute("ADMIN_EMAIL") == null) {
            return "redirect:/admin-login";
        }

        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("pendingRequests", resetRepo.findByStatus("PENDING"));
        model.addAttribute("approvedRequests", resetRepo.findByStatus("APPROVED"));
        model.addAttribute("rejectedRequests", resetRepo.findByStatus("REJECTED"));
        model.addAttribute("allResetRequests", resetRepo.findAll());

        return "admin-dashboard";
    }

    /* ================= ADMIN LOGOUT ================= */
    @GetMapping("/admin-logout")
    public String adminLogout(HttpSession session,
                              RedirectAttributes redirectAttributes) {

        session.invalidate();
        redirectAttributes.addFlashAttribute(
                "logoutMsg", "Admin logged out successfully."
        );
        return "redirect:/admin-login";
    }
}
