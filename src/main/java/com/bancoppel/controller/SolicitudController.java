package com.bancoppel.controller;

import com.bancoppel.dto.RespuestaOrquestador;
import com.bancoppel.dto.Solicitud;
import com.bancoppel.service.OrquestadorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/orquestador") // Esta será la ruta base en tu URL
@RequiredArgsConstructor
public class SolicitudController {

    private final OrquestadorService orquestadorService;

    @PostMapping("/test")
    public ResponseEntity<String> imprimirSolicitud(@RequestBody Solicitud solicitud) {

        // Imprimimos en la consola del IDE
        log.info("=== NUEVA SOLICITUD RECIBIDA ===");
        log.info("{}", solicitud);
        log.info("================================");

        return ResponseEntity.ok("JSON recibido y mostrado en consola correctamente.");
    }

    @PostMapping("/procesarUno")
    public ResponseEntity<String> procesarPeticionUno(@RequestBody Solicitud solicitud) {

        log.info("\n [Controller] Petición HTTP POST recibida.");

        // 3. Le pasamos la "pelota" (el DTO) a tu servicio para que haga el trabajo pesado
        orquestadorService.procesarSolicitud(solicitud);

        // 4. Respondemos al cliente (Postman) que todo salió bien
        return ResponseEntity.ok("La solicitud del cliente " + solicitud.numCliente() + " está siendo orquestada.");
    }

    @PostMapping("/procesar")
    public ResponseEntity<RespuestaOrquestador> procesarPeticion(@RequestBody Solicitud solicitud) {
        log.info(" Petición HTTP POST recibida para el cliente: {}", solicitud.numCliente());

        // Ahora el servicio nos devuelve el objeto armado
        RespuestaOrquestador respuesta = orquestadorService.procesarSolicitud(solicitud);

        return ResponseEntity.ok(respuesta);
    }

}
