package com.proyecto.servicios.client;

import com.proyecto.servicios.client.config.GestoPagoTxFeignConfig;
import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente OpenFeign para el envío de transacciones financieras hacia Gestopago (sendTx.do).
 */
@FeignClient(
        name = "gestoPagoTx",
        url = "${gestopago.tx.url:https://gestopago.portalventas.net}",
        configuration = GestoPagoTxFeignConfig.class
)
public interface GestoPagoTxClient {

    @PostMapping(
            value = "${gestopago.tx.ruta:/sistema/service/sendTx.do}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    GestoPagoTxResponse enviarTransaccion(@RequestBody GestoPagoTxRequest request);
}

