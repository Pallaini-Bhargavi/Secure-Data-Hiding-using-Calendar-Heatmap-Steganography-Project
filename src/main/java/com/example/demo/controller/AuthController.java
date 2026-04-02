package com.example.demo.controller;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.KeyService;
import com.example.demo.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyService keyService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailService mailService;
    
    // ================= REGISTER =================

    @GetMapping("/register")
    public String showRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String register(HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {

    try {
        String email = request.getParameter("email").trim().toLowerCase();
        String password = request.getParameter("password");
        String securityQuestion = request.getParameter("securityQuestion");
        String securityAnswer = request.getParameter("securityAnswer");

        if (userRepository.findByUserEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("alreadyRegistered", true);
            return "redirect:/register";
        }

        // 🔐 Generate ECC key ONCE
        KeyPair keyPair = keyService.generateECCKeyPair();

        User user = new User();
        user.setUserEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswerHash(
                passwordEncoder.encode(securityAnswer.trim().toLowerCase()));

        // ✅ STORE KEYS DIRECTLY
        user.setPublicKey(Base64.getEncoder()
                .encodeToString(keyPair.getPublic().getEncoded()));

        user.setEncryptedPrivateKey(Base64.getEncoder()
                .encodeToString(keyPair.getPrivate().getEncoded()));

        if (userRepository.existsByUserEmail(user.getUserEmail())) {
        throw new RuntimeException("User already exists");
    }


        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "registerSuccess", "Registration successful.");
        return "redirect:/login";

    } catch (Exception e) {
        redirectAttributes.addFlashAttribute(
                "registerError", "Registration failed");
        return "redirect:/register";
    }
}
    // ================= LOGIN =================

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

   @PostMapping("/login")
    @SuppressWarnings("CallToPrintStackTrace")
    public String login(@RequestParam String email,
                    @RequestParam String password,
                    RedirectAttributes redirectAttributes,
                    HttpSession session) {

    User user = userRepository
            .findByUserEmail(email.trim().toLowerCase())
            .orElse(null);

    if (user == null) {
        redirectAttributes.addFlashAttribute(
                "loginError",
                "Invalid email or password."
        );
        return "redirect:/login";
    }

    // 🔒 CHECK LOCK
    if (user.getLoginLockedUntil() != null &&
        user.getLoginLockedUntil().isAfter(LocalDateTime.now())) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        redirectAttributes.addFlashAttribute(
                "loginError",
                "Account locked until " +
                user.getLoginLockedUntil().format(formatter)
        );
        return "redirect:/login";
    }

    // WRONG PASSWORD
     if (!passwordEncoder.matches(password, user.getPasswordHash())) {

    int attempts = user.getLoginFailAttempts() + 1;
    user.setLoginFailAttempts(attempts);

    // ✅ SEND MAIL FOR EVERY WRONG ATTEMPT
    try {
    mailService.sendTextMail(
    user.getUserEmail(),
    "⚠️ Login Attempt Failed",
    """
    A failed login attempt was detected.
    
    Attempt: """ + attempts + " / 3\n" +
    "Time: " + LocalDateTime.now().toString()
);
} catch (Exception e) {
    
    e.printStackTrace();
}

    // 🔒 LOCK AFTER 3 ATTEMPTS
    if (attempts >= 3) {
        LocalDateTime lockUntil =
                LocalDateTime.now().plusHours(3);
        user.setLoginLockedUntil(lockUntil);

        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "loginError",
                "Account locked until " +
                lockUntil.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );

        return "redirect:/login";
    }

    // ✅ SAVE ATTEMPTS (IMPORTANT)
    userRepository.save(user);

    redirectAttributes.addFlashAttribute(
            "loginError",
            "Invalid password. Attempt " + attempts + " / 3"
    );

    return "redirect:/login";
}

    // ✅ SUCCESS LOGIN
    user.setLoginFailAttempts(0);
    user.setLoginLockedUntil(null);
    userRepository.save(user);

    session.setAttribute("USER_EMAIL", user.getUserEmail());
    session.setAttribute("PRIVATE_KEY", user.getEncryptedPrivateKey());
    redirectAttributes.addFlashAttribute("loginSuccess", true);
    return "redirect:/home";

}


    // ================= LOGOUT =================
@GetMapping("/logout")
public String logout(HttpSession session) {
    session.invalidate();   // destroy session
    return "redirect:/login?logout=success";
}

}

