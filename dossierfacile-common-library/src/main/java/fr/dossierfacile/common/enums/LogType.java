package fr.dossierfacile.common.enums;

public enum LogType {
    CUSTOM_EMAIL(Constants.TENANT_MESSAGES),
    ACCOUNT_CREATED(null),
    ACCOUNT_CREATED_VIA_KC(null),
    ACCOUNT_LINK(null),// first connection on DF
    ACCOUNT_COMPLETED(null),
    ACCOUNT_DENIED("operatorId"),
    ACCOUNT_EDITED(null),
    ACCOUNT_VALIDATED("operatorId"),
    ACCOUNT_COMPLETE("tenant"),
    ACCOUNT_MODIFICATION(Constants.TENANT_MESSAGES),
    ACCOUNT_DELETE(null),
    ACCOUNT_ARCHIVED(null),
    NEW_MESSAGE(Constants.TENANT_MESSAGES),
    EMAIL_ACCOUNT_VALIDATED(null),
    FIRST_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION(null),
    SECOND_ACCOUNT_WARNING_FOR_DOCUMENT_DELETION(null),
    DOCUMENT_DELETION_AFTER_2_ACCOUNT_WARNINGS(null),
    FC_ACCOUNT_CREATION(null),
    FC_ACCOUNT_LINK(null);

    String associateTab;

    LogType(String associateTab) {
        this.associateTab = associateTab;
    }

    public String getAssociateTab() {
        return associateTab;
    }

    private static class Constants {
        private static final String TENANT_MESSAGES = "tenant-message";
    }
}
