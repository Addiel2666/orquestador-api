package com.bancoppel.service;

import com.bancoppel.dto.JSONDestino;
import com.bancoppel.dto.RespuestaOrquestador;
import com.bancoppel.dto.ServicioExternoRespuesta;
import com.bancoppel.dto.Solicitud;
import com.bancoppel.entity.BitacoraRespuestaEntity;
import com.bancoppel.entity.BitacoraSolicitudEntity;
import com.bancoppel.entity.ProveedorEntity;
import com.bancoppel.repository.BitacoraRespuestaRepository;
import com.bancoppel.repository.BitacoraSolicitudRepository;
import com.bancoppel.repository.CatEstatusRepository;
import com.bancoppel.repository.ProveedorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrquestadorService {

    // 1. Inyectamos todos los repositorios y herramientas
    private final ProveedorRepository proveedorRepository;
    private final BitacoraSolicitudRepository solicitudRepository;
    private final BitacoraRespuestaRepository respuestaRepository;
    private final CatEstatusRepository estatusRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RespuestaOrquestador procesarSolicitud(Solicitud solicitud) {
        log.info("=== Iniciando orquestación para cliente: {} | Tel: {} ===", solicitud.numCliente(), solicitud.telefono());

        // 1. BUSCAR PROVEEDORES ACTIVOS (Catálogo)
        List<String> nombresAValidar = obtenerNombresProveedores(solicitud);
        List<ProveedorEntity> proveedoresBD = nombresAValidar.stream()
                .map(proveedorRepository::findByNombreAndActivoTrue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (proveedoresBD.isEmpty()) {
            return new RespuestaOrquestador("Rechazado: No se encontraron proveedores activos en la BD.", new ArrayList<>());
        }

        // 2. GUARDAR BITÁCORA SOLICITUD ("EN ESPERA" - ID 1)
        BitacoraSolicitudEntity bitacoraSoli = new BitacoraSolicitudEntity();
        bitacoraSoli.setNumCliente(String.valueOf(solicitud.numCliente()));
        bitacoraSoli.setTelefono(solicitud.telefono());
        bitacoraSoli.setTelCifrado(solicitud.telCifrado());
        bitacoraSoli.setNumSolicitud(java.util.UUID.randomUUID().toString());
        bitacoraSoli.setEstatus(estatusRepository.getReferenceById(1));

        try {
            bitacoraSoli.setJsonSolicitud(objectMapper.writeValueAsString(solicitud));
        } catch (Exception e) { log.error("Error serializando JSON", e); }

        final BitacoraSolicitudEntity solicitudGuardada = solicitudRepository.save(bitacoraSoli);

        // 3. SEPARAR PROVEEDORES: CACHEADOS VS A INVOCAR
        LocalDateTime hace20Dias = LocalDateTime.now().minusDays(20);
        String telefonoStr = String.valueOf(solicitud.telefono());

        List<ServicioExternoRespuesta> respuestasFinales = new ArrayList<>();
        List<ProveedorEntity> proveedoresAInvocar = new ArrayList<>();

        for (ProveedorEntity proveedor : proveedoresBD) {
            String nombreProv = proveedor.getNombre().toUpperCase();

            // Buscamos si hay una respuesta exitosa y vigente en BD
            var respuestaVigente = respuestaRepository
                    .findFirstByTelefonoAndProveedor_NombreAndFlagConsultaSatisfactoriaTrueAndCreatedAtAfterOrderByCreatedAtDesc(
                            telefonoStr, nombreProv, hace20Dias);

            if (respuestaVigente.isPresent()) {
                log.info("⚡ [{}] Dato vigente en BD encontrado (Cache Hit).", nombreProv);

                try {
                    // 1. Obtenemos la entidad original completa
                    BitacoraRespuestaEntity original = respuestaVigente.get();
                    String jsonGuardado = original.getJsonRespuesta();

                    Object jsonLimpio = objectMapper.readValue(jsonGuardado, Object.class);
                    respuestasFinales.add(new ServicioExternoRespuesta(nombreProv, jsonLimpio));

                    // 2. Creamos el clon para la auditoría de la nueva solicitud
                    BitacoraRespuestaEntity resClon = new BitacoraRespuestaEntity();
                    resClon.setBitacoraSolicitud(solicitudGuardada);
                    resClon.setProveedor(proveedor);
                    resClon.setNumCliente(solicitudGuardada.getNumCliente());
                    resClon.setTelefono(telefonoStr);
                    resClon.setTelCifrado(solicitud.telCifrado());
                    resClon.setCodigoEstatus(200);
                    resClon.setJsonRespuesta(jsonGuardado);
                    resClon.setFlagConsultaSatisfactoria(true);

                    // 🌟 LA CLAVE: Seteamos la respuesta original como "padre" de este clon
                    resClon.setRespuestaOrigen(original);

                    respuestaRepository.save(resClon);

                } catch (Exception e) {
                    log.error("Error procesando caché de {}. Forzando llamada real.", nombreProv);
                    proveedoresAInvocar.add(proveedor);
                }
            } else {
                // No hay caché válido, se debe consultar a la API real
                log.info(" [{}] No hay datos recientes. Se encolará para llamada externa.", nombreProv);
                proveedoresAInvocar.add(proveedor);
            }
        }

        // 4. EJECUTAR LLAMADAS (Solo a los que no estaban cacheados)
        List<JSONDestino> cuerpos = solicitud.servicioExterno() != null ? solicitud.servicioExterno() : new ArrayList<>();

        if (!proveedoresAInvocar.isEmpty()) {
            List<ServicioExternoRespuesta> respuestasHilos = ejecutarLlamadasEnParalelo(proveedoresAInvocar, cuerpos, solicitudGuardada, solicitud);
            respuestasFinales.addAll(respuestasHilos); // Juntamos las reales con las cacheadas
        }

        // 5. ACTUALIZAR ESTATUS A "ATENDIDO" (ID 2)
        solicitudGuardada.setEstatus(estatusRepository.getReferenceById(2));
        solicitudRepository.save(solicitudGuardada);
        log.info(" Solicitud ID: {} actualizada a estatus: ATENDIDO", solicitudGuardada.getIdSolicitud());

        return new RespuestaOrquestador("Orquestación Finalizada Exitosamente", respuestasFinales);
    }

    private List<ServicioExternoRespuesta> ejecutarLlamadasEnParalelo(
            List<ProveedorEntity> proveedoresBD,
            List<JSONDestino> serviciosJSON,
            BitacoraSolicitudEntity solicitudPadre,
            Solicitud solicitudOriginal) {

        Map<String, Object> mapaCuerpos = serviciosJSON.stream()
                .collect(Collectors.toMap(s -> s.nombreServicio().toUpperCase(), JSONDestino::cuerpo));

        List<ServicioExternoRespuesta> resultados = new java.util.concurrent.CopyOnWriteArrayList<>();

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {

            List<java.util.concurrent.CompletableFuture<Void>> tareasEnEjecucion = new ArrayList<>();

            for (ProveedorEntity proveedor : proveedoresBD) {
                String nombreUpper = proveedor.getNombre().toUpperCase();

                if (mapaCuerpos.containsKey(nombreUpper)) {
                    var tarea = java.util.concurrent.CompletableFuture.runAsync(() -> {

                        // Instanciamos el registro de bitácora respuesta
                        BitacoraRespuestaEntity resEnt = new BitacoraRespuestaEntity();
                        resEnt.setBitacoraSolicitud(solicitudPadre);
                        resEnt.setProveedor(proveedor);
                        resEnt.setNumCliente(solicitudPadre.getNumCliente());
                        resEnt.setTelefono(String.valueOf(solicitudOriginal.telefono()));
                        resEnt.setTelCifrado(solicitudOriginal.telCifrado());

                        try {
                            // Armando URL (Ej: http://127.0.0.1:8081)
                            //String url = "http://" + proveedor.getIpDominio() + ":" + proveedor.getPuerto();
                            log.info("Hilo: {} disparando a {} en {}", Thread.currentThread().getName(), nombreUpper, proveedor.getIpDominio());

                            ResponseEntity<String> response = restTemplate.postForEntity(proveedor.getIpDominio(), mapaCuerpos.get(nombreUpper), String.class);

                            // Llenando Bitácora (Éxito)
                            resEnt.setCodigoEstatus(response.getStatusCode().value());
                            resEnt.setJsonRespuesta(response.getBody());
                            resEnt.setFlagConsultaSatisfactoria(true);

                            Object jsonLimpio = objectMapper.readValue(response.getBody(), Object.class);
                            resultados.add(new ServicioExternoRespuesta(nombreUpper, jsonLimpio));

                        } catch (Exception e) {
                            log.error(" Fallo en {}: {}", nombreUpper, e.getMessage());

                            // Llenando Bitácora (Fallo)
                            resEnt.setCodigoEstatus(500);
                            resEnt.setJsonRespuesta("Error: " + e.getMessage());
                            resEnt.setFlagConsultaSatisfactoria(false);

                            resultados.add(new ServicioExternoRespuesta(nombreUpper, "Error de comunicación con el proveedor"));
                        }

                        //  GUARDAMOS LA RESPUESTA EN BD
                        respuestaRepository.save(resEnt);

                    }, executor);
                    tareasEnEjecucion.add(tarea);
                } else {
                    log.warn("El cuerpo del JSON para el proveedor {} no está incluido", nombreUpper);
                }
            }

            java.util.concurrent.CompletableFuture.allOf(
                    tareasEnEjecucion.toArray(new java.util.concurrent.CompletableFuture[0])
            ).join();

        } catch(Exception ex){
            log.error("Ocurrió un error en el executor {}", ex.getMessage());
        }

        return resultados;
    }

    private List<String> obtenerNombresProveedores(Solicitud solicitud) {
        List<String> proveedores = new ArrayList<>();
        if (Boolean.TRUE.equals(solicitud.consultaMonnai())) proveedores.add("MONNAI");
        if (Boolean.TRUE.equals(solicitud.consultaGrandata())) proveedores.add("GRANDATA");
        if (Boolean.TRUE.equals(solicitud.consultaBemobi())) proveedores.add("BEMOBI");
        return proveedores;
    }
}