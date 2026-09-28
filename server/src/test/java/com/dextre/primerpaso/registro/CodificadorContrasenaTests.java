package com.dextre.primerpaso.registro;

import java.util.Base64;
import java.util.stream.Stream;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodificadorContrasenaTests {

    private final CodificadorContrasena codificador = new CodificadorContrasena();

    @Test
    void generaHashConSalIndependienteSinGuardarTextoOriginal() {
        String contrasena = "Clave segura 123";
        String primerHash = codificador.codificar(contrasena);
        String segundoHash = codificador.codificar(contrasena);

        assertThat(primerHash).startsWith("{pbkdf2-sha256}600000$")
                .doesNotContain(contrasena).isNotEqualTo(segundoHash);
        String[] primeraPartes = primerHash.split("\\$");
        String[] segundaPartes = segundoHash.split("\\$");
        assertThat(primeraPartes).hasSize(3);
        assertThat(segundaPartes).hasSize(3);
        assertThat(primeraPartes[1]).isNotEqualTo(segundaPartes[1]);
        assertThat(Base64.getDecoder().decode(primeraPartes[1])).hasSize(16);
        assertThat(Base64.getDecoder().decode(segundaPartes[1])).hasSize(16);
        assertThat(Base64.getDecoder().decode(primeraPartes[2])).hasSize(32);
        assertThat(Base64.getDecoder().decode(segundaPartes[2])).hasSize(32);
    }

    @Test
    void permiteVerificarHashConEspaciosYCaracteresUnicodeIntactos() throws Exception {
        String contrasena = "  contraseña Perú 123  ";
        String hash = codificador.codificar(contrasena);
        String[] partes = hash.split("\\$");
        int iteraciones = Integer.parseInt(partes[0].substring("{pbkdf2-sha256}".length()));
        byte[] sal = Base64.getDecoder().decode(partes[1]);
        byte[] hashGuardado = Base64.getDecoder().decode(partes[2]);

        assertThat(derivar(contrasena, sal, iteraciones)).isEqualTo(hashGuardado);
        assertThat(derivar(contrasena.trim(), sal, iteraciones)).isNotEqualTo(hashGuardado);
    }

    @ParameterizedTest
    @MethodSource("contrasenasInvalidas")
    void rechazaContrasenasFueraDelContrato(String contrasena) {
        assertThatThrownBy(() -> codificador.codificar(contrasena))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La contraseña debe tener entre 8 y 128 caracteres.");
    }

    static Stream<String> contrasenasInvalidas() {
        return Stream.of(null, "", "        ", "corta12", "x".repeat(129));
    }

    private byte[] derivar(String contrasena, byte[] sal, int iteraciones) throws Exception {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena.toCharArray(), sal, iteraciones, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacion).getEncoded();
        } finally {
            especificacion.clearPassword();
        }
    }
}
