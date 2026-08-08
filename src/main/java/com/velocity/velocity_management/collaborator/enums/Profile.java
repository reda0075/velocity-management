package com.velocity.velocity_management.collaborator.enums;

public enum Profile {
    DEV("DEV"),
    DEVOPS("DO"),
    QA("QA");

    private final String code;

    Profile(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
