package com.finance.application.ops;

/** Port: delivers a short operational message to whoever runs the service. Never carries personal or financial data. */
public interface AlertNotifier {

    void send(String title, String message);
}
