package com.softy.be.common.api;

public record ApiResponse<T>(
        boolean success,
        int code,
        String message,
        T data
) {
    public static <T> ApiResponse<T> of(boolean success, int code, String message, T data) {
        return new ApiResponse<>(success, code, message, data);
    }
}
