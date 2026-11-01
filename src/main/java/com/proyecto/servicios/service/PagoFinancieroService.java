package com.proyecto.servicios.service;

import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;

import java.time.LocalDateTime;

/**
 * Servicio de orquestación transaccional y financiera de pagos.
 */
public interface PagoFinancieroService {

    /**
     * Procesa un pago de servicios o recarga electrónica bajo el patrón de tres fases:
     * validación contra catálogo, bloqueo pesimista y reserva local, consumo a Gestopago (sendTx.do),
     * y liquidación, custodia por timeout o reversión definitiva según el resultado.
     *
     * @param request datos completos de la transacción financiera
     * @return respuesta consolidada de la transacción
     */
    PagoResponse procesarPago(PagoRequest request);

    /**
     * Consulta el estatus de una transacción por su folio o clave de idempotencia,
     * verificando que pertenezca al usuario autenticado.
     *
     * @param request identificador de búsqueda en cuerpo DTO
     * @return información actual de la transacción
     */
    PagoResponse consultarTransaccion(ConsultaTransaccionRequest request);

    /**
     * Ejecuta la recuperación controlada de transacciones pendientes que quedaron
     * en estado {@code EN_PROCESO} antes del umbral especificado.
     *
     * @param umbral fecha/hora límite para considerar huérfana una operación en proceso
     * @return número de transacciones procesadas en la recuperación
     */
    default int recuperarTransaccionesPendientes(LocalDateTime umbral) {
        return 0;
    }

    /**
     * Concilia una transacción en estado {@code TIMEOUT} una vez confirmado su resultado
     * con el proveedor externo.
     */
    default PagoResponse conciliarTransaccionEnCustodia(Long txId,
                                                        boolean confirmadaExitosaEnProveedor,
                                                        String folioProveedor,
                                                        String motivoAuditoria) {
        throw new UnsupportedOperationException("No implementado");
    }
}
