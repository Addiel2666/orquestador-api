package com.bancoppel.dto;

import java.util.List;

public record Solicitud (

        String tipoSolicitud, //es el origen de donde proviene la solicitud
        Boolean consultaMonnai,
        Boolean consultaGrandata,
        Boolean consultaBemobi,

        String numSolicitud,
        String numCliente,
        String telefono,
        String telCifrado,

        List<JSONDestino> servicioExterno
) {

    public Solicitud {
        // Si el valor no venía en el JSON, Jackson lo pone en null.
        // Aquí lo interceptamos y le asignamos false por defecto.
        if (consultaMonnai == null) consultaMonnai = false;
        if (consultaGrandata == null) consultaGrandata = false;
        if (consultaBemobi == null) consultaBemobi = false;
    }
}
