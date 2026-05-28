package com.psychosim.simulation.domain.service;

import java.util.List;

/**
 * Resultado de la validación del mundo por WorldValidationService.
 * Registro de dominio puro; sin dependencias de Spring.
 */
public record WorldValidationResult(List<WorldValidationIssue> issues) {

    public boolean hasErrors() {
        return issues.stream().anyMatch(i -> i.severity() == Severity.ERROR);
    }

    public boolean canPublish() {
        return !hasErrors();
    }

    public List<WorldValidationIssue> errors() {
        return issues.stream().filter(i -> i.severity() == Severity.ERROR).toList();
    }

    public List<WorldValidationIssue> warnings() {
        return issues.stream().filter(i -> i.severity() == Severity.WARNING).toList();
    }

    public enum Severity {
        ERROR, WARNING
    }

    public record WorldValidationIssue(
            Severity severity,
            String code,
            String message,
            String entityRef
    ) {}
}
