package com.proyecto.servicios.service;

import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;

/**
 * Servicio de orquestación transaccional y financiera de pagos.
 */
public interface PagoFinancieroService {

    /**
     * Procesa un pago de servicios o recarga electrónica bajo el patrón de dos fases:
     * validación, bloqueo pesimista y reserva local, consumo a Gestopago (sendTx.do),
     * y liquidación o reversión definitiva según la respuesta del proveedor.
     *
     * @param request datos completos de la transacción financiera
     * @return respuesta consolidada de la transacción
     */
    PagoResponse procesarPago(PagoRequest request);

    /**
     * Consulta el estatus de una transacción por su folio o clave de idempotencia.
     *
     * @param request identificador de búsqueda en cuerpo DTO
     * @return información actual de la transacción
     */
    PagoResponse consultarTransaccion(ConsultaTransaccionRequest request);
}

