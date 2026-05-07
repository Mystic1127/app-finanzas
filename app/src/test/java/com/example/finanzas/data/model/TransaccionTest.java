package com.example.finanzas.data.model;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class TransaccionTest {
    @Test
    public void displayNote_hidesRecurringMarkerInsideTransferNote() {
        Transaccion tx = new Transaccion(
                1,
                99,
                Transaccion.TRANSFER_CATEGORY,
                false,
                25.0,
                "PEN",
                new Date(0L),
                "CASH",
                "[transfer:CARD] [recurrent:7:2026-05-07] Depósito - Caja chica"
        );

        assertEquals("Depósito - Caja chica", tx.getDisplayNote());
    }

    @Test
    public void displayNote_hidesRecurringMarkerForRegularTransaction() {
        Transaccion tx = new Transaccion(
                2,
                3,
                "Alimentación",
                false,
                12.5,
                "PEN",
                new Date(0L),
                "CARD",
                "[recurrent:2:2026-05-07] Almuerzo"
        );

        assertEquals("Almuerzo", tx.getDisplayNote());
    }

    @Test
    public void transferDestination_readsStoredDestinationAccount() {
        Transaccion tx = new Transaccion(
                3,
                99,
                Transaccion.TRANSFER_CATEGORY,
                false,
                100.0,
                "PEN",
                new Date(0L),
                "CASH",
                "[transfer:ACCOUNT_123] Deposito - Ahorro"
        );

        assertEquals("ACCOUNT_123", tx.getTransferDestinationAccountType());
    }
}
