package com.example.demo.controller;

import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EncodeRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.AESUtil;
import com.example.demo.security.ECCService;
import com.example.demo.service.MailService;
import com.example.demo.stego.EncodePipelineService;
import com.example.demo.stego.HeatmapImageRenderer;
import com.example.demo.stego.HeatmapLayout;
import com.example.demo.stego.PngMetadataUtil;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/encode")
public class EncodeApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ECCService eccService;

    @Autowired
    private EncodePipelineService pipelineService;

    @Autowired
    private MailService mailService;

    @PostMapping(value = "/generate", produces = "image/png")
        public ResponseEntity<?> generateHeatmap(
        @RequestBody EncodeRequest request,
        HttpSession session) throws Exception {

    String senderEmail =
            (String) session.getAttribute("USER_EMAIL");
    String base64PrivateKey =
            (String) session.getAttribute("PRIVATE_KEY");

    if (senderEmail == null || base64PrivateKey == null) {
        return ResponseEntity.status(401).build();
    }

    if (senderEmail.equalsIgnoreCase(request.getReceiverEmail())) {
        return ResponseEntity
                .badRequest()
                .body("Sender and receiver email cannot be same.");
    }

    User sender = userRepository
            .findByUserEmail(senderEmail)
            .orElseThrow();

    User receiver = userRepository
            .findByUserEmail(request.getReceiverEmail())
            .orElse(null);

    if (receiver == null) {
        return ResponseEntity
                .status(404)
                .body("Receiver email does not exist.");
    }

    long startTime = System.currentTimeMillis();

    byte[] senderPrivateKey =
            Base64.getDecoder().decode(base64PrivateKey);

    byte[] receiverPublicKey =
            Base64.getDecoder().decode(receiver.getPublicKey());

    byte[] sharedSecret =
            eccService.generateSharedSecret(
                    senderPrivateKey,
                    receiverPublicKey);

    byte[] aesKey =
            AESUtil.deriveAESKey(sharedSecret);

    String securedText =
        "CHS::" + request.getPlaintext();

    byte[] plaintextBytes =
        securedText.getBytes("UTF-8");

    List<HeatmapLayout> layouts =
            pipelineService.encode(plaintextBytes, aesKey);

    BufferedImage image =
            HeatmapImageRenderer.render(layouts);

    byte[] png =
            PngMetadataUtil.writeWithMetadata(
                    image,
                    sender.getPublicKey(), 
                    layouts.size());

    session.setAttribute("PREVIEW_HEATMAP", png);
    session.setAttribute("PREVIEW_RECEIVER", receiver.getUserEmail());
double messageKB = Math.round((plaintextBytes.length / 1024.0) * 100.0) / 100.0;
double heatmapKB = Math.round((png.length / 1024.0) * 100.0) / 100.0;

long endTime = System.currentTimeMillis();
long encodingTime = endTime - startTime;

System.out.println("\n===== PERFORMANCE METRICS =====");
System.out.println("MESSAGE SIZE (KB): " + messageKB);
System.out.println("HEATMAP SIZE (KB): " + heatmapKB);
System.out.println("ENCODING TIME (ms): " + encodingTime);
System.out.println("===== ENCODING DONE =====");

    return ResponseEntity
            .ok()
            .header("Content-Type", "image/png")
            .body(png);
}

@PostMapping("/send")
public ResponseEntity<?> sendHeatmap(HttpSession session) throws Exception {

    byte[] png =
            (byte[]) session.getAttribute("PREVIEW_HEATMAP");
    String receiverEmail =
            (String) session.getAttribute("PREVIEW_RECEIVER");
    String senderEmail =
            (String) session.getAttribute("USER_EMAIL");

    if (png == null || receiverEmail == null) {
        return ResponseEntity
                .badRequest()
                .body("No heatmap to send.");
    }

    // ✅ receiver → image
    mailService.sendHeatmapMail(
            receiverEmail,
            "You received a secure heatmap",
            "You have received a secure calendar heatmap image.",
            png
    );

    // ✅ sender → TEXT ONLY
    mailService.sendTextMail(
            senderEmail,
            "Heatmap sent successfully",
            "Your heatmap was successfully sent to " + receiverEmail
    );

    // cleanup
    session.removeAttribute("PREVIEW_HEATMAP");
    session.removeAttribute("PREVIEW_RECEIVER");
    
    return ResponseEntity.ok().build();
}
}
