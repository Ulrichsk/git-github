package com.kfokam48.tickets.exception;

/**
 * Signale une violation d'une regle metier (transition de statut interdite,
 * modification d'un ticket cloture, etc.). Traduite en HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
