package fr.dossierfacile.common.enums;

public enum TenantFileStatus {

    TO_PROCESS("non vérifié"),
    VALIDATED("vérifié"),
    DECLINED("modification demandée"),
    INCOMPLETE("non terminé"),
    COMPLETED("complété"),
    ARCHIVED("");


    private final String label;

    TenantFileStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * A dossier can be shared by link or mail once complete and submitted:
     * either verified by an operator (VALIDATED) or completed without
     * operator verification (COMPLETED).
     */
    public boolean isCompletedOrValidated() {
        return this == VALIDATED || this == COMPLETED;
    }

    /**
     * Complete dossier (all mandatory documents + honor declaration) on its way to
     * verification: COMPLETED → TO_PROCESS → VALIDATED. Excludes INCOMPLETE, DECLINED, ARCHIVED.
     */
    public boolean isCompletedOrBetter() {
        return this == COMPLETED || this == TO_PROCESS || this == VALIDATED;
    }
}
