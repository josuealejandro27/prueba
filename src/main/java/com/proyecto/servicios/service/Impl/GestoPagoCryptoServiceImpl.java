package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.service.GestoPagoCryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Service
@Slf4j
public class GestoPagoCryptoServiceImpl implements GestoPagoCryptoService {

    private final String rawKey;
    private final String transformation;
    private final String rawIv;

    public GestoPagoCryptoServiceImpl(
            @Value("${gestopago.tx.aes.key:${gestopago.auth.password:gestopago_default_secure_key_2026}}") String rawKey,
            @Value("${gestopago.tx.aes.transformation:AES/CBC/PKCS5Padding}") String transformation,
            @Value("${gestopago.tx.aes.iv:0123456789abcdef}") String rawIv) {
        this.rawKey = rawKey;
        this.transformation = transformation;
        this.rawIv = rawIv;
    }

    @Override
    public String firmar(GestoPagoTxRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo para generar la firma");
        }

        // Construir cadena canónica de campos según el payload acordado en la arquitectura
        String cadenaCanonica = String.format("%s|%s|%s|%s|%s|%s|%s",
                valorOSeguro(request.getIdProveedor()),
                valorOSeguro(request.getIdService()),
                valorOSeguro(request.getTelefono()),
                valorOSeguro(request.getMontoPago()),
                valorOSeguro(request.getUpc()),
                valorOSeguro(request.getUnidad()),
                valorOSeguro(request.getHoraLocal())
        );

        return cifrar(cadenaCanonica);
    }

    @Override
    public String cifrar(String contenidoPlano) {
        if (contenidoPlano == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec secretKey = obtenerClaveSecreta();

            if (transformation.toUpperCase().contains("CBC")) {
                IvParameterSpec ivSpec = obtenerVectorInicializacion();
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            }

            byte[] bytesCifrados = cipher.doFinal(contenidoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytesCifrados);

        } catch (Exception e) {
            log.error("Error al cifrar contenido con AES [transformación={}]: {}", transformation, e.getMessage());
            throw new IllegalStateException("Error al procesar cifrado de transacción: " + e.getMessage(), e);
        }
    }

    @Override
    public String descifrar(String contenidoCifradoBase64) {
        if (contenidoCifradoBase64 == null || contenidoCifradoBase64.isBlank()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec secretKey = obtenerClaveSecreta();

            if (transformation.toUpperCase().contains("CBC")) {
                IvParameterSpec ivSpec = obtenerVectorInicializacion();
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            }

            byte[] bytesCifrados = Base64.getDecoder().decode(contenidoCifradoBase64);
            byte[] bytesOriginales = cipher.doFinal(bytesCifrados);
            return new String(bytesOriginales, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error al descifrar contenido con AES [transformación={}]: {}", transformation, e.getMessage());
            throw new IllegalStateException("Error al procesar descifrado de transacción: " + e.getMessage(), e);
        }
    }

    private SecretKeySpec obtenerClaveSecreta() {
        try {
            // Normalizar a 256 bits (32 bytes) usando SHA-256 para cualquier longitud de clave configurada
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en JVM", e);
        }
    }

    private IvParameterSpec obtenerVectorInicializacion() {
        byte[] ivBytes = rawIv != null ? rawIv.getBytes(StandardCharsets.UTF_8) : new byte[16];
        if (ivBytes.length != 16) {
            ivBytes = Arrays.copyOf(ivBytes, 16);
        }
        return new IvParameterSpec(ivBytes);
    }

    private String valorOSeguro(Object valor) {
        return valor != null ? valor.toString().trim() : "";
    }
}

