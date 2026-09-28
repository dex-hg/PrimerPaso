# PrimerPaso

El registro de postulantes y empresas guarda cuentas y perfiles en PostgreSQL. El frontend estático está en `client` y Spring Boot en `server`. Spring sirve ambos desde `http://localhost:8080` cuando se ejecuta dentro de `server`.

## Ejecución local

Requisitos: Java 21 o superior, PostgreSQL y la base de datos `PrimerPaso` con las tablas del proyecto.

1. Copiar `server/.env.example` como `server/.env` y completar usuario y contraseña. El archivo usa formato Java properties: los valores no llevan comillas; si una contraseña contiene una barra invertida, escribirla como `\\`.
2. Para una base nueva, ejecutar `db/primerpaso.sql` una sola vez. Para una base con el esquema anterior, ejecutar únicamente `db/registro_reclutador.sql`. Esta ampliación ya se aplicó a la base local durante la integración y admite repetirse.
3. Abrir una terminal **dentro de `server`** y ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

4. Abrir `http://localhost:8080`. No es necesario iniciar un segundo servidor para el frontend.

`.env` está excluido de Git. `.env.example` contiene únicamente valores de ejemplo. La aplicación comprueba la conexión y las tablas al arrancar; no crea ni modifica el esquema automáticamente.

Si se utiliza Live Server u otro servidor estático, se admiten los orígenes locales de los puertos 5500, 4173 y 8000. Se pueden cambiar mediante `PRIMERPASO_CORS_ORIGENES` con una lista de orígenes separados por comas. El acceso recomendado es el que sirve Spring en el puerto 8080.

## Registro disponible

`POST /api/registro/postulantes` recibe nombres, apellidos, correo, contraseña, aceptación de términos, institución, carrera, condición académica, ciclo o año de egreso, habilidades, intereses y consentimiento de comunicaciones. Un estudiante debe indicar un ciclo entre 1 y 30; un egresado o titulado, un año entre 1950 y el año actual.

`POST /api/registro/empresas` recibe los datos de la empresa y del reclutador, país, identificación fiscal, sector, ciudad, sitio web opcional e intereses. Para Perú se exige un RUC de 11 dígitos. La cuenta del reclutador recibe la membresía de administrador de la empresa registrada.

La cuenta y su perfil, membresía y preferencias se guardan en una transacción. Los correos se normalizan a minúsculas; las contraseñas conservan sus caracteres y se almacenan con PBKDF2-HMAC-SHA-256, sal aleatoria de 16 bytes y 600 000 iteraciones. La API devuelve `201` al crear, `400` para datos inválidos, `409` para correo o identificación fiscal duplicados y `503` cuando el servicio no puede completar la operación.

Las carreras escritas en el formulario y las opciones seleccionadas se resuelven en sus catálogos sin duplicar nombres. Un catálogo inactivo impide el registro y revierte la transacción. `terms_version_user` usa `registro-v1` para identificar esta versión del formulario; el contenido legal de los enlaces de términos sigue pendiente.

El inicio de sesión todavía es una vista demostrativa. El registro no crea una sesión. La carga de CV, la publicación de la primera vacante y el acceso social se muestran deshabilitados hasta sus siguientes etapas.

## Verificación

Dentro de `server`:

```powershell
.\mvnw.cmd verify
```

Las pruebas automatizadas cubren validación, contrato HTTP, respuestas de error sin datos internos, normalización, contraseñas, catálogos y rollback. El contexto de prueba no depende de las credenciales ni de una instancia PostgreSQL disponible.

La configuración externa sigue la [documentación de Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html). La transacción de registro aplica el rollback de excepciones de ejecución definido por [Spring Framework](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html).
