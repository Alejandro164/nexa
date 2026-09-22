package com.chavescr.nexa.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cifrado simétrico (AES-256-GCM) para secretos que se guardan en base de datos, como tokens de API. */
@Component
public class CifradoService {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANO_IV_BYTES = 12;
    private static final int TAMANO_TAG_BITS = 128;

    private final SecretKeySpec clave;

    public CifradoService(@Value("${whatsapp.encryption.key}") String claveBase64) {
        byte[] bytesClave;
        try {
            bytesClave = Base64.getDecoder().decode(claveBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("whatsapp.encryption.key debe estar codificada en Base64", e);
        }
        if (bytesClave.length != 32) {
            throw new IllegalStateException(
                    "whatsapp.encryption.key debe ser una clave AES-256 (32 bytes) codificada en Base64");
        }
        this.clave = new SecretKeySpec(bytesClave, "AES");
    }

    public String cifrar(String textoPlano) {
        try {
            byte[] iv = new byte[TAMANO_IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cifrado.length);
            buffer.put(iv).put(cifrado);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el valor", e);
        }
    }

    public String descifrar(String textoCifrado) {
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifrado);
            byte[] iv = Arrays.copyOfRange(datos, 0, TAMANO_IV_BYTES);
            byte[] cifrado = Arrays.copyOfRange(datos, TAMANO_IV_BYTES, datos.length);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el valor", e);
        }
    }
}
