package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;

/**
 * Servicio criptográfico para firma y cifrado de transacciones financieras con Gestopago.
 */
public interface GestoPagoCryptoService {

    /**
     * Genera la firma/token criptográfico AES para el payload de la transacción.
     *
     * @param request datos de la transacción
     * @return firma digital en formato Base64
     */
    String firmar(GestoPagoTxRequest request);

    /**
     * Cifra una cadena de texto en claro mediante el algoritmo AES configurado.
     *
     * @param contenidoPlano texto a cifrar
     * @return texto cifrado en Base64
     */
    String cifrar(String contenidoPlano);

    /**
     * Descifra una cadena previamente cifrada con AES en Base64.
     *
     * @param contenidoCifradoBase64 texto cifrado en Base64
     * @return texto en claro
     */
    String descifrar(String contenidoCifradoBase64);
}

