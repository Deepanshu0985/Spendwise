package com.finance.infrastructure.web.statement;

/** password (optional) is only for opening an encrypted PDF; it is never stored, logged or echoed back. */
public record RetryStatementRequest(String password) {

    @Override
    public String toString() {
        return "RetryStatementRequest[password=" + (password == null ? "none" : "hidden") + "]";
    }
}
