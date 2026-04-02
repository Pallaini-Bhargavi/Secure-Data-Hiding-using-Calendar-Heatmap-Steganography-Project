package com.example.demo.controller;

import java.util.Base64;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.repository.UserRepository;
import com.example.demo.security.AESUtil;
import com.example.demo.security.ECCService;
import com.example.demo.stego.DecodePipelineService;
import com.example.demo.stego.PngMetadataUtil;

import jakarta.servlet.http.HttpSession;

@RestController
public class DecodeApiController {

    @Autowired
    private ECCService eccService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DecodePipelineService decodePipeline;

    @Autowired
    private PasswordEncoder passwordEncoder;

   @PostMapping("/api/decode/image")
        public ResponseEntity<String> decodeImage(
        @RequestParam("image") MultipartFile image,
        @RequestParam("password") String password,
        HttpSession session) {
    
    try {
        long startTime = System.currentTimeMillis();
        // ================= SESSION =================
        String email = (String) session.getAttribute("USER_EMAIL");
        String base64PrivateKey =
                (String) session.getAttribute("PRIVATE_KEY");


        if (email == null || base64PrivateKey == null) {

            return ResponseEntity.status(401).body("Session Expired. Please login again.");
        }

        var user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        boolean passwordMatch =
                passwordEncoder.matches(password, user.getPasswordHash());

        if (!passwordMatch) {
            return ResponseEntity
                    .status(403)
                    .body("Incorrect password. Please try again.");
        }
        var reader =
                ImageIO.getImageReadersByFormatName("png").next();

        reader.setInput(
                ImageIO.createImageInputStream(
                        image.getInputStream()));

        IIOMetadata metadata =
                reader.getImageMetadata(0);

        String senderPubBase64 =
                PngMetadataUtil.getSenderPublicKey(metadata);

        int symbolCount =
                PngMetadataUtil.getSymbolCount(metadata);


        if (senderPubBase64 == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Invalid Image.Please try again.");
        }

        byte[] receiverPrivateKey =
                Base64.getDecoder().decode(base64PrivateKey);

        byte[] senderPublicKey =
                Base64.getDecoder().decode(senderPubBase64);

        byte[] sharedSecret =
        eccService.generateSharedSecretForDecoding(
                receiverPrivateKey,
                senderPublicKey);

        byte[] aesKey =
                AESUtil.deriveAESKey(sharedSecret);

        String plaintext =
                decodePipeline.decode(
                        image, aesKey, symbolCount);

        
        if (!plaintext.startsWith("CHS::")) {
    return ResponseEntity
            .status(403)
            .body("You are not authorized to decode this message.");
}

// strip marker
        plaintext = plaintext.substring(5);
        long endTime = System.currentTimeMillis();
long decodingTime = endTime - startTime;

double messageKB = Math.round((plaintext.length() / 1024.0) * 100.0) / 100.0;

System.out.println("\n===== DECODING METRICS =====");
System.out.println("MESSAGE SIZE (KB): " + messageKB);
System.out.println("DECODING TIME (ms): " + decodingTime);
System.out.println("===== DECODING DONE =====");
        return ResponseEntity.ok(plaintext);

    } catch (Exception e) {
        return ResponseEntity
                .badRequest()
                .body("Decoding failed. Please try again.");
    }
}
}
