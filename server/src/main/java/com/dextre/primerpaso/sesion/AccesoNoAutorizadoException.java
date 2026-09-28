package com.dextre.primerpaso.sesion;

public class AccesoNoAutorizadoException extends RuntimeException {

    public AccesoNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
