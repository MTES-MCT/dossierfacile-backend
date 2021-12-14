package fr.dossierfacile.common.enums;

public enum LogType {
    CUSTOM_EMAIL(Constants.TENANT_MESSAGES),
    ACCOUNT_CREATED(null),
    ACCOUNT_COMPLETED(null),
    ACCOUNT_DENIED("operatorId"),
    ACCOUNT_EDITED(null),
    ACCOUNT_VALIDATED("operatorId"),
    ACCOUNT_COMPLETE("tenant"),
    ACCOUNT_MODIFICATION(Constants.TENANT_MESSAGES),
    ACCOUNT_DELETE(null),
    NEW_MESSAGE(Constants.TENANT_MESSAGES),
    ADD_PROPERTY_OWNER(null),
    VISIT_PROPERTY_OWNER(null),
    REMOVE_PROPERTY_APARTMENT_SHARING_OWNER(null),
    REMOVE_PROPERTY_OWNER(null),
    EMAIL_ACCOUNT_VALIDATED(null);

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
