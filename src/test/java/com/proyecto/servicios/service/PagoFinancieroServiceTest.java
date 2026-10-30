package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.mongo.CatalogoProductosCache;
import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.TipoOperacion;
import com.proyecto.servicios.exception.CuentaException;
import com.proyecto.servicios.exception.TransaccionIdempotenciaException;
import com.proyecto.servicios.model.catalogo.ProductoDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.transacciones.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transacciones.PagoRequest;
import com.proyecto.servicios.model.transacciones.PagoResponse;
import com.proyecto.servicios.repositorys.mongo.CatalogoProductosCacheRepository;
import com.proyecto.servicios.repositorys.transacciones.TransaccionFinancieraRepository;
import com.proyecto.servicios.service.Impl.PagoFinancieroServiceImpl;
import com.proyecto.servicios.service.Impl.TransaccionPersistenceHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de orquestación, idempotencia y validación de catálogo en PagoFinancieroService")
class PagoFinancieroServiceTest {

    @Mock
    private TransaccionPersistenceHelper persistenceHelper;

    @Mock
    private GestoPagoCryptoService cryptoService;

    @Mock
    private GestoPagoTxClient txClient;

    @Mock
    private TransaccionFinancieraRepository transaccionRepository;

    @Mock
    private CatalogoProductosCacheRepository cacheRepository;

    @Mock
    private GestoPagoTokenService tokenService;

    private PagoFinancieroServiceImpl pagoService;

    private PagoRequest pagoRequest;
    private TransaccionFinanciera txEnProceso;
    private CuentaBancaria cuenta;
    private PersonaFisica titular;

    @BeforeEach
    void setUp() {
        pagoService = new PagoFinancieroServiceImpl(
                persistenceHelper, cryptoService, txClient, transaccionRepository,
                cacheRepository, tokenService, 83
        );

        titular = new PersonaFisica();
        titular.setId(1L);
        titular.setCorreo("titular@example.com");
        titular.setActivo(true);

        cuenta = new CuentaBancaria();
        cuenta.setId(1L);
        cuenta.setNumeroCuenta("1234567890123456");
        cuenta.setSaldo(new BigDecimal("400.00"));
        cuenta.setEstatus("ACTIVA");
        cuenta.setPersonaFisica(titular);

        pagoRequest = PagoRequest.builder()
                .numeroCuenta("1234567890123456")
                .idServicio(105L)
                .idProducto(201L)
                .monto(new BigDecimal("100.00"))
                .idDistribuidor(83)
                .referencia("5512345678")
                .bandera("T")
                .pin("Password123!")
                .upc("123456789012")
                .idempotencyKey("idemp-test-999")
                .build();

        txEnProceso = TransaccionFinanciera.builder()
                .id(10L)
                .folio("folio-uuid-10")
                .idempotencyKey("idemp-test-999")
                .cuenta(cuenta)
                .monto(new BigDecimal("100.00"))
                .tipoOperacion(TipoOperacion.PAGO_SERVICIO)
                .estado(EstadoTransaccion.EN_PROCESO)
                .idDistribuidor(83)
                .idServicio(105L)
                .idProducto(201L)
                .referencia("5512345678")
                .fechaCreacion(LocalDateTime.now())
                .build();

        ProductoDTO productoValido = ProductoDTO.builder()
                .idServicio(105L)
                .idProducto(201L)
                .producto("Recarga 100")
                .precio(new BigDecimal("100.00"))
                .upc("123456789012")
                .tipoReferencia("t")
                .activo(true)
                .build();

        CatalogoProductosCache cache = CatalogoProductosCache.builder()
                .id(CatalogoProductosCache.LLAVE_UNICA)
                .fechaActualizacion(LocalDateTime.now())
                .productos(List.of(productoValido))
                .build();

        lenient().when(cacheRepository.findById(CatalogoProductosCache.LLAVE_UNICA))
                .thenReturn(Optional.of(cache));
    }

    @Test
    @DisplayName("Flujo exitoso código 0: Gestopago aprueba y consolida el débito")
    void procesarPago_ExitoConGestopagoCodigo0_DebitaSaldoYMarcaExitosa() {
        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest)).thenReturn(Optional.empty());
        when(persistenceHelper.verificarIdempotencia("idemp-test-999")).thenReturn(Optional.empty());
        when(persistenceHelper.iniciarYReservarSaldo(pagoRequest)).thenReturn(txEnProceso);
        when(cryptoService.firmar(any(GestoPagoTxRequest.class))).thenReturn("firma-aes-base64");

        GestoPagoTxResponse responseGp = GestoPagoTxResponse.builder()
                .codigo("0")
                .mensaje("TRANSACCION EXITOSA")
                .folio("AUT-112233")
                .build();

        when(txClient.enviarTransaccion(any(GestoPagoTxRequest.class))).thenReturn(responseGp);

        TransaccionFinanciera txExitosa = TransaccionFinanciera.builder()
                .id(10L)
                .folio("folio-uuid-10")
                .idempotencyKey("idemp-test-999")
                .cuenta(cuenta)
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.EXITOSA)
                .codigoRespuestaProveedor("0")
                .folioProveedor("AUT-112233")
                .build();

        when(persistenceHelper.liquidarExito(eq(10L), eq(responseGp))).thenReturn(txExitosa);

        PagoResponse esperado = PagoResponse.builder()
                .folio("folio-uuid-10")
                .estado(EstadoTransaccion.EXITOSA)
                .codigoRespuestaProveedor("0")
                .folioProveedor("AUT-112233")
                .saldoRestante(new BigDecimal("400.00"))
                .build();

        when(persistenceHelper.construirPagoResponse(txExitosa)).thenReturn(esperado);

        PagoResponse resultado = pagoService.procesarPago(pagoRequest);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        assertThat(resultado.getCodigoRespuestaProveedor()).isEqualTo("0");

        verify(persistenceHelper).marcarEnviandoAProveedor(10L);
        verify(persistenceHelper).liquidarExito(eq(10L), eq(responseGp));
        verify(persistenceHelper, never()).liquidarRechazo(any(), any());
    }

    @Test
    @DisplayName("Flujo exitoso código 01: Gestopago aprueba según especificación")
    void procesarPago_ExitoConGestopagoCodigo01_DebitaSaldoYMarcaExitosa() {
        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest)).thenReturn(Optional.empty());
        when(persistenceHelper.verificarIdempotencia("idemp-test-999")).thenReturn(Optional.empty());
        when(persistenceHelper.iniciarYReservarSaldo(pagoRequest)).thenReturn(txEnProceso);
        when(cryptoService.firmar(any(GestoPagoTxRequest.class))).thenReturn("firma-aes-base64");

        GestoPagoTxResponse responseGp = GestoPagoTxResponse.builder()
                .codigo("01")
                .mensaje("APROBADA")
                .folio("AUT-445566")
                .build();

        when(txClient.enviarTransaccion(any(GestoPagoTxRequest.class))).thenReturn(responseGp);

        TransaccionFinanciera txExitosa = TransaccionFinanciera.builder()
                .id(10L)
                .folio("folio-uuid-10")
                .idempotencyKey("idemp-test-999")
                .cuenta(cuenta)
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.EXITOSA)
                .codigoRespuestaProveedor("01")
                .build();

        when(persistenceHelper.liquidarExito(eq(10L), eq(responseGp))).thenReturn(txExitosa);

        PagoResponse esperado = PagoResponse.builder()
                .folio("folio-uuid-10")
                .estado(EstadoTransaccion.EXITOSA)
                .codigoRespuestaProveedor("01")
                .build();

        when(persistenceHelper.construirPagoResponse(txExitosa)).thenReturn(esperado);

        PagoResponse resultado = pagoService.procesarPago(pagoRequest);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        assertThat(resultado.getCodigoRespuestaProveedor()).isEqualTo("01");
        verify(persistenceHelper).liquidarExito(eq(10L), eq(responseGp));
    }

    @Test
    @DisplayName("Flujo rechazo por Gestopago: reversa el saldo reservado y marca RECHAZADA")
    void procesarPago_RechazoPorGestopago_ReversaSaldoYMarcaRechazada() {
        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest)).thenReturn(Optional.empty());
        when(persistenceHelper.verificarIdempotencia("idemp-test-999")).thenReturn(Optional.empty());
        when(persistenceHelper.iniciarYReservarSaldo(pagoRequest)).thenReturn(txEnProceso);
        when(cryptoService.firmar(any(GestoPagoTxRequest.class))).thenReturn("firma-aes-base64");

        GestoPagoTxResponse responseGp = GestoPagoTxResponse.builder()
                .codigo("ERR-99")
                .mensaje("NUMERO TELEFONICO NO PERTENECE A LA COMPANIA")
                .build();

        when(txClient.enviarTransaccion(any(GestoPagoTxRequest.class))).thenReturn(responseGp);

        TransaccionFinanciera txRechazada = TransaccionFinanciera.builder()
                .id(10L)
                .folio("folio-uuid-10")
                .idempotencyKey("idemp-test-999")
                .cuenta(cuenta)
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.RECHAZADA)
                .codigoRespuestaProveedor("ERR-99")
                .build();

        when(persistenceHelper.liquidarRechazo(eq(10L), eq(responseGp))).thenReturn(txRechazada);

        PagoResponse esperado = PagoResponse.builder()
                .folio("folio-uuid-10")
                .estado(EstadoTransaccion.RECHAZADA)
                .codigoRespuestaProveedor("ERR-99")
                .saldoRestante(new BigDecimal("500.00"))
                .build();

        when(persistenceHelper.construirPagoResponse(txRechazada)).thenReturn(esperado);

        PagoResponse resultado = pagoService.procesarPago(pagoRequest);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.RECHAZADA);
        assertThat(resultado.getCodigoRespuestaProveedor()).isEqualTo("ERR-99");
        verify(persistenceHelper).liquidarRechazo(eq(10L), eq(responseGp));
        verify(persistenceHelper, never()).liquidarExito(any(), any());
    }

    @Test
    @DisplayName("Falla por Timeout en llamada externa: marca TIMEOUT y pone en custodia sin liberar saldo")
    void procesarPago_TimeoutEnGestopago_MarcaTimeoutSinLiberarSaldo() {
        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest)).thenReturn(Optional.empty());
        when(persistenceHelper.verificarIdempotencia("idemp-test-999")).thenReturn(Optional.empty());
        when(persistenceHelper.iniciarYReservarSaldo(pagoRequest)).thenReturn(txEnProceso);
        when(cryptoService.firmar(any(GestoPagoTxRequest.class))).thenReturn("firma-aes-base64");

        when(txClient.enviarTransaccion(any(GestoPagoTxRequest.class)))
                .thenThrow(new RuntimeException("Timeout", new SocketTimeoutException("Read timed out")));

        TransaccionFinanciera txTimeout = TransaccionFinanciera.builder()
                .id(10L)
                .folio("folio-uuid-10")
                .estado(EstadoTransaccion.TIMEOUT)
                .build();

        when(persistenceHelper.marcarTimeout(eq(10L), any())).thenReturn(txTimeout);

        PagoResponse esperado = PagoResponse.builder()
                .folio("folio-uuid-10")
                .estado(EstadoTransaccion.TIMEOUT)
                .build();

        when(persistenceHelper.construirPagoResponse(txTimeout)).thenReturn(esperado);

        PagoResponse resultado = pagoService.procesarPago(pagoRequest);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.TIMEOUT);
        verify(persistenceHelper).marcarTimeout(eq(10L), any());
        verify(persistenceHelper, never()).liquidarRechazo(any(), any());
    }

    @Test
    @DisplayName("Idempotencia EXITOSA: si la transacción previa ya fue exitosa, no vuelve a llamar a Gestopago")
    void procesarPago_IdempotenciaExitosa_RetornaTransaccionPreviaSinLlamarGestopago() {
        PagoResponse previaExitosa = PagoResponse.builder()
                .folio("folio-previo-existente")
                .idempotencyKey("idemp-test-999")
                .numeroCuenta("1234567890123456")
                .estado(EstadoTransaccion.EXITOSA)
                .codigoRespuestaProveedor("0")
                .monto(new BigDecimal("100.00"))
                .build();

        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest)).thenReturn(Optional.empty());
        when(persistenceHelper.verificarIdempotencia("idemp-test-999")).thenReturn(Optional.of(previaExitosa));

        PagoResponse resultado = pagoService.procesarPago(pagoRequest);

        assertThat(resultado.getFolio()).isEqualTo("folio-previo-existente");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);

        verify(persistenceHelper, never()).iniciarYReservarSaldo(any());
        verify(txClient, never()).enviarTransaccion(any());
    }

    @Test
    @DisplayName("Idempotencia RECHAZADA: devuelve resultado original sin volver a cobrar ni reembolsar dos veces")
    void procesarPago_IdempotenciaRechazada_RetornaResultadoOriginalSinRepetirCobroNiReembolso() {
        TransaccionFinanciera txPreviaRechazada = TransaccionFinanciera.builder()
                .id(20L)
                .folio("folio-rechazado-20")
                .idempotencyKey("idemp-test-999")
                .cuenta(cuenta)
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.RECHAZADA)
                .codigoRespuestaProveedor("80")
                .mensajeProveedor("RECHAZO PREVIO")
                .build();

        PagoResponse respuestaOriginal = PagoResponse.builder()
                .folio("folio-rechazado-20")
                .idempotencyKey("idemp-test-999")
                .numeroCuenta("1234567890123456")
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.RECHAZADA)
                .codigoRespuestaProveedor("80")
                .saldoRestante(new BigDecimal("400.00"))
                .build();

        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest))
                .thenReturn(Optional.of(txPreviaRechazada));
        when(persistenceHelper.construirPagoResponse(txPreviaRechazada)).thenReturn(respuestaOriginal);

        PagoResponse resultado = pagoService.procesarPago(pagoRequest);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.RECHAZADA);
        assertThat(resultado.getFolio()).isEqualTo("folio-rechazado-20");

        verify(persistenceHelper, never()).iniciarYReservarSaldo(any());
        verify(txClient, never()).enviarTransaccion(any());
        verify(persistenceHelper, never()).liquidarRechazo(any(), any());
        verify(persistenceHelper, never()).liquidarExito(any(), any());
    }

    @Test
    @DisplayName("Idempotencia TIMEOUT: conserva resultado incierto y nunca vuelve a enviar cobro a Gestopago")
    void procesarPago_IdempotenciaTimeout_NoVuelveACobrarAlProveedor() {
        TransaccionFinanciera txPreviaTimeout = TransaccionFinanciera.builder()
                .id(30L)
                .folio("folio-timeout-30")
                .idempotencyKey("idemp-test-999")
                .cuenta(cuenta)
                .monto(new BigDecimal("100.00"))
                .estado(EstadoTransaccion.TIMEOUT)
                .codigoRespuestaProveedor("TIMEOUT")
                .build();

        PagoResponse respuestaOriginal = PagoResponse.builder()
                .folio("folio-timeout-30")
                .idempotencyKey("idemp-test-999")
                .estado(EstadoTransaccion.TIMEOUT)
                .build();

        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest))
                .thenReturn(Optional.of(txPreviaTimeout));
        when(persistenceHelper.construirPagoResponse(txPreviaTimeout)).thenReturn(respuestaOriginal);

        PagoResponse resultado = pagoService.procesarPago(pagoRequest);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.TIMEOUT);
        verify(persistenceHelper, never()).iniciarYReservarSaldo(any());
        verify(txClient, never()).enviarTransaccion(any());
        verify(persistenceHelper, never()).liquidarRechazo(any(), any());
    }

    @Test
    @DisplayName("Idempotencia EN_PROCESO: rechaza con conflicto indicando operación aún en ejecución")
    void procesarPago_IdempotenciaEnProceso_LanzaTransaccionIdempotenciaException() {
        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest))
                .thenReturn(Optional.of(txEnProceso));

        assertThatThrownBy(() -> pagoService.procesarPago(pagoRequest))
                .isInstanceOf(TransaccionIdempotenciaException.class);

        verify(txClient, never()).enviarTransaccion(any());
    }

    @Test
    @DisplayName("Validación de catálogo: rechaza monto manipulado distinto al precio oficial del catálogo")
    void procesarPago_MontoManipuladoDistintoAlCatalogo_LanzaCuentaException() {
        pagoRequest.setMonto(new BigDecimal("0.01")); // Manipulado desde frontend (catálogo = 100.00)
        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest)).thenReturn(Optional.empty());
        when(persistenceHelper.verificarIdempotencia("idemp-test-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.procesarPago(pagoRequest))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("PRECIO_DIFERENTE_CATALOGO");

        verify(persistenceHelper, never()).iniciarYReservarSaldo(any());
        verify(txClient, never()).enviarTransaccion(any());
    }

    @Test
    @DisplayName("Validación de catálogo: rechaza producto inexistente")
    void procesarPago_ProductoInexistente_LanzaCuentaException() {
        pagoRequest.setIdProducto(99999L);
        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest)).thenReturn(Optional.empty());
        when(persistenceHelper.verificarIdempotencia("idemp-test-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.procesarPago(pagoRequest))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("PRODUCTO_INEXISTENTE");

        verify(persistenceHelper, never()).iniciarYReservarSaldo(any());
        verify(txClient, never()).enviarTransaccion(any());
    }

    @Test
    @DisplayName("Validación de catálogo: rechaza producto inactivo")
    void procesarPago_ProductoInactivo_LanzaCuentaException() {
        ProductoDTO inactivo = ProductoDTO.builder()
                .idServicio(105L)
                .idProducto(201L)
                .precio(new BigDecimal("100.00"))
                .activo(false)
                .build();
        CatalogoProductosCache cache = CatalogoProductosCache.builder()
                .id(CatalogoProductosCache.LLAVE_UNICA)
                .fechaActualizacion(LocalDateTime.now())
                .productos(List.of(inactivo))
                .build();
        when(cacheRepository.findById(CatalogoProductosCache.LLAVE_UNICA)).thenReturn(Optional.of(cache));
        when(persistenceHelper.buscarPorIdempotenciaValidandoConsistencia(pagoRequest)).thenReturn(Optional.empty());
        when(persistenceHelper.verificarIdempotencia("idemp-test-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.procesarPago(pagoRequest))
                .isInstanceOf(CuentaException.class)
                .extracting(ex -> ((CuentaException) ex).getCode())
                .isEqualTo("PRODUCTO_INACTIVO");

        verify(persistenceHelper, never()).iniciarYReservarSaldo(any());
        verify(txClient, never()).enviarTransaccion(any());
    }

    @Test
    @DisplayName("Consultar: recupera, valida pertenencia de la cuenta y devuelve la transacción")
    void consultarTransaccion_FolioExistente_RetornaResponse() {
        ConsultaTransaccionRequest request = new ConsultaTransaccionRequest("folio-uuid-10");
        when(transaccionRepository.findByFolio("folio-uuid-10")).thenReturn(Optional.of(txEnProceso));

        PagoResponse esperado = PagoResponse.builder()
                .folio("folio-uuid-10")
                .estado(EstadoTransaccion.EN_PROCESO)
                .build();

        when(persistenceHelper.construirPagoResponse(txEnProceso)).thenReturn(esperado);

        PagoResponse resultado = pagoService.consultarTransaccion(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getFolio()).isEqualTo("folio-uuid-10");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.EN_PROCESO);
        verify(persistenceHelper).validarPropiedadCuenta(cuenta);
    }
}
