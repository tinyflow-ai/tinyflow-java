package com.tinyflow.demo.common;

public class Result {

    private final Integer code;
    private final String msg;
    private final Object data;

    public Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result success(Object data) {
        return new Result(200, "成功", data);
    }

    public static Result success() {
        return success(null);
    }
}
