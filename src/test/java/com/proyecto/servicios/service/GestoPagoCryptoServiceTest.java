package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.service.Impl.GestoPagoCryptoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestoPagoCryptoServiceTest {

    private GestoPagoCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new GestoPagoCryptoServiceImpl(
                "mi_clave_secreta_super_segura_aes",
                "AES/CBC/PKCS5Padding",
                "0123456789abcdef"
        );
    }

    @Test
    @DisplayName("Cifrado y descifrado: el texto recuperado es idéntico al texto original")
    void cifrarYDescifrar_MantieneIntegridadDelTexto() {
        String mensaje = "Transaccion:cuenta=1234567890123456,monto=150.00,ref=5512345678";

        String cifrado = cryptoService.cifrar(mensaje);

        assertThat(cifrado).isNotNull();
        assertThat(cifrado).isNotEqualTo(mensaje);

        String descifrado = cryptoService.descifrar(cifrado);
        assertThat(descifrado).isEqualTo(mensaje);
    }

    @Test
    @DisplayName("Firmar payload: genera una firma Base64 válida que al descifrar contiene la cadena canónica")
    void firmar_GeneraFirmaValidaYDescifrable() {
        GestoPagoTxRequest request = GestoPagoTxRequest.builder()
                .idProveedor(83)
                .idService(105L)
                .telefono("5512345678")
                .montoPago(new BigDecimal("100.00"))
                .upc("123456789012")
                .unidad("1")
                .horaLocal("2026-10-28 22:00:00")
                .build();

        String firma = cryptoService.firmar(request);

        assertThat(firma).isNotNull().isNotBlank();

        String canonicalDescifrada = cryptoService.descifrar(firma);
        assertThat(canonicalDescifrada).contains("83|105|5512345678|100.00|123456789012|1|2026-10-28 22:00:00");
    }

    @Test
    @DisplayName("Firmar con request nulo lanza IllegalArgumentException")
    void firmar_ConRequestNulo_LanzaExcepcion() {
        assertThatThrownBy(() -> cryptoService.firmar(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Cifrar contenido nulo devuelve null de forma segura")
    void cifrar_ConTextoNulo_RetornaNull() {
        assertThat(cryptoService.cifrar(null)).isNull();
        assertThat(cryptoService.descifrar(null)).isNull();
    }
}

