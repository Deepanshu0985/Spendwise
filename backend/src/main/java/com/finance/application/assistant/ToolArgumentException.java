package com.finance.application.assistant;

/** The model asked for a tool with arguments that failed validation. Not an HTTP error: it is reported back to the model as the tool's result. */
public class ToolArgumentException extends RuntimeException {

    public ToolArgumentException(String message) {
        super(message);
    }
}
