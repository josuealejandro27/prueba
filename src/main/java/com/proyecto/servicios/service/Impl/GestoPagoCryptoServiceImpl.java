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
import java.util.Base64;
import java.util.Set;

@Service
@Slf4j
public class GestoPagoCryptoServiceImpl implements GestoPagoCryptoService {

    private static final int MIN_KEY_LENGTH = 16;
    private static final int AES_CBC_IV_BYTES = 16;
    private static final Set<String> CLAVES_INSEGURAS_PROHIBIDAS = Set.of(
            "gestopago_default_secure_key_2026",
            "clave_aes_ficticia_ejemplo_12345",
            "changeme",
            "default"
    );

    private final String rawKey;
    private final String transformation;
    private final String rawIv;

    public GestoPagoCryptoServiceImpl(
            @Value("${gestopago.tx.aes.key:}") String rawKey,
            @Value("${gestopago.tx.aes.transformation:AES/CBC/PKCS5Padding}") String transformation,
            @Value("${gestopago.tx.aes.iv:}") String rawIv) {
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
        validarConfiguracionCriptografica();
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

        } catch (IllegalStateException e) {
            throw e;
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
        validarConfiguracionCriptografica();
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

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al descifrar contenido con AES [transformación={}]: {}", transformation, e.getMessage());
            throw new IllegalStateException("Error al procesar descifrado de transacción: " + e.getMessage(), e);
        }
    }

    public void validarConfiguracionCriptografica() {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("La clave AES de Gestopago (GESTOPAGO_AES_KEY) es obligatoria y no puede estar vacía");
        }
        String keyLimpia = rawKey.trim();
        if (keyLimpia.length() < MIN_KEY_LENGTH || CLAVES_INSEGURAS_PROHIBIDAS.contains(keyLimpia.toLowerCase())) {
            throw new IllegalStateException("La clave AES de Gestopago no cumple los requisitos mínimos de seguridad y longitud");
        }
        if (transformation == null || transformation.isBlank()) {
            throw new IllegalStateException("La transformación criptográfica AES no está configurada");
        }
        if (transformation.toUpperCase().contains("CBC")) {
            if (rawIv == null || rawIv.isBlank()) {
                throw new IllegalStateException("El vector de inicialización AES (GESTOPAGO_AES_IV) es obligatorio para el modo CBC");
            }
            byte[] ivBytes = rawIv.getBytes(StandardCharsets.UTF_8);
            if (ivBytes.length != AES_CBC_IV_BYTES) {
                throw new IllegalStateException("El vector de inicialización AES (GESTOPAGO_AES_IV) debe tener exactamente 16 bytes");
            }
        }
    }

    private SecretKeySpec obtenerClaveSecreta() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(rawKey.trim().getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en JVM", e);
        }
    }

    private IvParameterSpec obtenerVectorInicializacion() {
        byte[] ivBytes = rawIv.getBytes(StandardCharsets.UTF_8);
        return new IvParameterSpec(ivBytes);
    }

    private String valorOSeguro(Object valor) {
        return valor != null ? valor.toString().trim() : "";
    }
}
