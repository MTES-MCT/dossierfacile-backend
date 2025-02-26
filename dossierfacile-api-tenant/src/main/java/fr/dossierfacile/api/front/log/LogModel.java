package fr.dossierfacile.api.front.log;

import ch.qos.logback.classic.Level;
import lombok.Getter;

@Getter
public class LogModel {
    String message;
    Level level;

    public LogModel(String message, Level level) {
        this.message = message;
        this.level = level;
    }
}
