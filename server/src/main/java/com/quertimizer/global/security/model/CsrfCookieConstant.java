package com.quertimizer.global.security.model;

public final class CsrfCookieConstant {

    public static final String PRODUCTION_API_HOST = "server.quertimizer.com";
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    public static final String EXPIRED_HOST_CSRF_COOKIE =
            "XSRF-TOKEN=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Secure; SameSite=Lax";
    public static final String EXPIRED_API_DOMAIN_CSRF_COOKIE =
            "XSRF-TOKEN=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Domain=server.quertimizer.com; Path=/; Secure; SameSite=Lax";

    private CsrfCookieConstant() {
    }

}
