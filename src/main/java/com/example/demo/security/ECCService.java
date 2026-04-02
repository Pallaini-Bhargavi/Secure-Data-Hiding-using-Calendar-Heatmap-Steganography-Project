package com.example.demo.security;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.KeyAgreement;

import org.springframework.stereotype.Service;

@Service
public class ECCService {
    public byte[] generateSharedSecret(
            byte[] senderPrivateKeyBytes,
            byte[] receiverPublicKeyBytes) throws Exception {

        System.out.println("\n=== ENCODING START ===");

        System.out.println("Sender Private Key :");
        System.out.println(Base64.getEncoder()
                .encodeToString(senderPrivateKeyBytes));

        System.out.println("Receiver Public Key :");
        System.out.println(Base64.getEncoder()
                .encodeToString(receiverPublicKeyBytes));

        KeyFactory keyFactory = KeyFactory.getInstance("EC");

        PrivateKey privateKey = keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(senderPrivateKeyBytes));

        PublicKey publicKey = keyFactory.generatePublic(
                new X509EncodedKeySpec(receiverPublicKeyBytes));

        KeyAgreement keyAgreement =
                KeyAgreement.getInstance("ECDH");

        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);

        byte[] sharedSecret =
                keyAgreement.generateSecret();

        System.out.println("ECC Shared Secret :");
        System.out.println(Base64.getEncoder()
                .encodeToString(sharedSecret));

        byte[] aesKey =
        AESUtil.deriveAESKey(sharedSecret);
    
         System.out.println("AES KEY :");
         System.out.println(Base64.getEncoder().encodeToString(aesKey));

        return sharedSecret;
    }
    public byte[] generateSharedSecretForDecoding(
        byte[] receiverPrivateKeyBytes,
        byte[] senderPublicKeyBytes) throws Exception {

    System.out.println("=== DECODING START ===");

    System.out.println("Receiver Private Key :");
    System.out.println(Base64.getEncoder()
            .encodeToString(receiverPrivateKeyBytes));

    System.out.println("Sender Public Key :");
    System.out.println(Base64.getEncoder()
            .encodeToString(senderPublicKeyBytes));

    KeyFactory keyFactory = KeyFactory.getInstance("EC");

    PrivateKey privateKey = keyFactory.generatePrivate(
            new PKCS8EncodedKeySpec(receiverPrivateKeyBytes));

    PublicKey publicKey = keyFactory.generatePublic(
            new X509EncodedKeySpec(senderPublicKeyBytes));

    KeyAgreement keyAgreement =
            KeyAgreement.getInstance("ECDH");

    keyAgreement.init(privateKey);
    keyAgreement.doPhase(publicKey, true);

    byte[] sharedSecret =
            keyAgreement.generateSecret();

    System.out.println("ECC Shared Secret :");
    System.out.println(Base64.getEncoder()
            .encodeToString(sharedSecret));

    byte[] aesKey =
        AESUtil.deriveAESKey(sharedSecret);
    
    System.out.println("AES KEY :");
    System.out.println(Base64.getEncoder().encodeToString(aesKey));

    return sharedSecret;
}
}
