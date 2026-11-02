package com.demo.restaurante.web;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ControlIntentosLoginTest {
    private static class RelojMutable extends Clock {
        private Instant ahora = Instant.parse("2026-11-01T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
        void avanzar(Duration tiempo) { ahora = ahora.plus(tiempo); }
    }

    @Test
    void bloqueaCincoFallosPorQuinceMinutosYLuegoPermiteReintentar() {
        RelojMutable reloj = new RelojMutable();
        ControlIntentosLogin control = new ControlIntentosLogin(reloj);
        String ip = "192.0.2.10";
        for (int i = 0; i < 4; i++) {
            assertFalse(control.estaBloqueado(ip));
            control.registrarFallo(ip);
        }
        assertFalse(control.estaBloqueado(ip));
        control.registrarFallo(ip);
        assertTrue(control.estaBloqueado(ip));
        assertFalse(control.estaBloqueado("192.0.2.11"));
        reloj.avanzar(Duration.ofMinutes(14));
        assertTrue(control.estaBloqueado(ip));
        reloj.avanzar(Duration.ofMinutes(1));
        assertFalse(control.estaBloqueado(ip));
        control.registrarFallo(ip);
        assertFalse(control.estaBloqueado(ip));
    }

    @Test
    void ventanaDeFallosCaducaYElExitoLimpiaContador() {
        RelojMutable reloj = new RelojMutable();
        ControlIntentosLogin control = new ControlIntentosLogin(reloj);
        for (int i = 0; i < 4; i++) {
            control.registrarFallo("198.51.100.1");
        }
        control.limpiar("198.51.100.1");
        control.registrarFallo("198.51.100.1");
        assertFalse(control.estaBloqueado("198.51.100.1"));
        reloj.avanzar(Duration.ofMinutes(16));
        for (int i = 0; i < 4; i++) {
            control.registrarFallo("198.51.100.1");
        }
        assertFalse(control.estaBloqueado("198.51.100.1"));
        control.registrarFallo("198.51.100.1");
        assertTrue(control.estaBloqueado("198.51.100.1"));
    }
}
