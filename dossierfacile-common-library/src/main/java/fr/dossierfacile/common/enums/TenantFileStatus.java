package fr.dossierfacile.common.enums;

public enum TenantFileStatus {

    TO_PROCESS("non vérifié"),
    VALIDATED("vérifié"),
    DECLINED("modification demandée"),
    INCOMPLETE("non terminé"),
    ARCHIVED("");


    String label;

    TenantFileStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
