package com.bancoppel.dto;

import java.util.List;

public record RespuestaOrquestador(
        // Agrego esta variable raíz para tus mensajes de "Paro Rápido" o "Éxito".
        // Después podrás agregarle más variables a este Record si lo necesitas.
        String mensajeSistema,
        List<ServicioExternoRespuesta> servicioExterno

) {
}
