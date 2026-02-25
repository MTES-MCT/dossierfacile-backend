package fr.dossierfacile.common.enums;

public enum FeatureAssignmentSource {
    HASH, // Used when the assignment is calculated with a bucket
    PRE_DEPLOYMENT; // Used when the feature is activated only for new users.
}

