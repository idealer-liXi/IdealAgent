package com.idealagent.types.result;

public record Result<T>(String code, String message, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>("0000", "success", data);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>("0001", message, null);
    }
}
