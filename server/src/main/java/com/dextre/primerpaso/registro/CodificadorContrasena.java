package com.dextre.primerpaso.registro;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

@Component
public class CodificadorContrasena {

    private static final int ITERACIONES = 600_000;
    private final SecureRandom aleatorio = new SecureRandom();

    public String codificar(String contrasena) {
        if (contrasena == null || contrasena.length() < 8 || contrasena.length() > 128
                || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 128 caracteres.");
        }
        byte[] sal = new byte[16];
        aleatorio.nextBytes(sal);
        char[] caracteres = contrasena.toCharArray();
        PBEKeySpec especificacion = new PBEKeySpec(caracteres, sal, ITERACIONES, 256);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacion).getEncoded();
            Base64.Encoder codificador = Base64.getEncoder();
            return "{pbkdf2-sha256}" + ITERACIONES + "$" + codificador.encodeToString(sal)
                    + "$" + codificador.encodeToString(hash);
        } catch (GeneralSecurityException excepcion) {
            throw new IllegalStateException("No se pudo proteger la contraseña.", excepcion);
        } finally {
            especificacion.clearPassword();
            Arrays.fill(caracteres, '\0');
        }
    }
}
