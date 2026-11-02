package com.demo.restaurante.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Límite temporal por dirección remota REAL de la conexión (no confía en
 * X-Forwarded-For). Adecuado para demostración local en un solo proceso.
 * No distingue usuarios: evita divulgar si una cuenta existe.
 */
@Service
public final class ControlIntentosLogin {
    static final int MAX_INTENTOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);
    static final Duration BLOQUEO = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Estado> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    private record Estado(int fallos, Instant desde, Instant hastaBloqueo) {
        boolean bloqueado(Instant ahora) {
            return hastaBloqueo != null && ahora.isBefore(hastaBloqueo);
        }
        boolean caducado(Instant ahora) {
            if (hastaBloqueo != null) {
                return !ahora.isBefore(hastaBloqueo);
            }
            return !ahora.isBefore(desde.plus(VENTANA));
        }
    }

    public ControlIntentosLogin() {
        this(Clock.systemUTC());
    }

    // Reloj inyectable para probar expiraciones sin esperas reales.
    ControlIntentosLogin(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean estaBloqueado(String direccion) {
        String clave = normalizar(direccion);
        Instant ahora = reloj.instant();
        Estado estado = registros.get(clave);
        if (estado == null) {
            return false;
        }
        if (estado.caducado(ahora)) {
            registros.remove(clave, estado);
            return false;
        }
        return estado.bloqueado(ahora);
    }

    public void registrarFallo(String direccion) {
        String clave = normalizar(direccion);
        Instant ahora = reloj.instant();
        // Limpia registros caducados para evitar crecimiento sin límite por uso normal.
        if (registros.size() > 512) {
            registros.entrySet().removeIf(e -> e.getValue().caducado(ahora));
        }
        registros.compute(clave, (key, anterior) -> {
            if (anterior == null || anterior.caducado(ahora)) {
                return new Estado(1, ahora, null);
            }
            if (anterior.bloqueado(ahora)) {
                return anterior;
            }
            int fallos = anterior.fallos() + 1;
            return fallos >= MAX_INTENTOS
                    ? new Estado(fallos, anterior.desde(), ahora.plus(BLOQUEO))
                    : new Estado(fallos, anterior.desde(), null);
        });
    }

    public void limpiar(String direccion) {
        registros.remove(normalizar(direccion));
    }

    private static String normalizar(String direccion) {
        return direccion == null || direccion.isBlank() ? "sin-direccion" : direccion;
    }
}
