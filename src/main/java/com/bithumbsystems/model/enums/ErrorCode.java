package com.bithumbsystems.model.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    UNKNOWN_ERROR(999, "알 수 없는 에러가 발생하였습니다. 운영자에게 문의 주시기 바랍니다!!!"),
    INVALID_HEADER_USER_IP(901, "Header 정보가 유효하지 않습니다!!(Not found user_ip)"),
    INVALID_HEADER_SITE_ID(902,"Header 정보가 유효하지 않습니다!!(Not found site_id)"),
    INVALID_HEADER_TOKEN(903, "Token 정보가 잘 못되었습니다!!!"),
    EXPIRED_TOKEN(909, "Token expired"),
    SERVER_RESPONSE_ERROR(904, "API 서버에서 에러가 발생하였습니다!!!"),
    GATEWAY_SERVER_ERROR(905, "Gateway Server Error"),
    USER_ALREADY_LOGIN(906, "User is already login"),
    INVALID_DOMAIN(910, "Invalid Domain Name"),
    INVALID_ORIGIN_DOMAIN(911, "Invalid Origin Name"),

    INVALID_URI_PATH(912, "Invalid URI Path");

    private final int code;
    private final String message;

    public static ErrorCode findByCode(int code) {
        return Arrays.stream(ErrorCode.values())
                .filter(errorCode -> errorCode.hasCode(code))
                .findAny()
                .orElse(UNKNOWN_ERROR);
    }

    public static ErrorCode findByName(String message) {
        return Arrays.stream(ErrorCode.values())
                .filter(errorName -> errorName.hasMessage(message))
                .findAny()
                .orElse(UNKNOWN_ERROR);
    }

    public boolean hasCode(int code) {
        return Arrays.stream(ErrorCode.values()).anyMatch(errorCode -> errorCode.code == code);
    }
    public boolean hasMessage(String message) {
        return Arrays.stream(ErrorCode.values()).anyMatch(errorCode -> errorCode.message.equals(message));
    }
}
