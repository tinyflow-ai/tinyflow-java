package dev.tinyflow.core.chain;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

public class ExceptionSummary implements Serializable {

    private String exceptionClass;
    private String message;
    private String stackTrace;

    private String rootCauseClass;
    private String rootCauseMessage;

    private String chainId;
    private String nodeId;

    private String errorCode; // 可选

    private long timestamp;

    public ExceptionSummary(Throwable error) {
        this.exceptionClass = error.getClass().getName();
        this.message = error.getMessage();
        this.stackTrace = getStackTraceAsString(error);

        Throwable root = getRootCause(error);
        this.rootCauseClass = root.getClass().getName();
        this.rootCauseMessage = root.getMessage();

        this.timestamp = System.currentTimeMillis();
    }

    private static Throwable getRootCause(Throwable t) {
        Throwable result = t;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    private static String getStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getRootCauseClass() {
        return rootCauseClass;
    }

    public void setRootCauseClass(String rootCauseClass) {
        this.rootCauseClass = rootCauseClass;
    }

    public String getRootCauseMessage() {
        return rootCauseMessage;
    }

    public void setRootCauseMessage(String rootCauseMessage) {
        this.rootCauseMessage = rootCauseMessage;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

