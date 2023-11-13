package io.levelops.ingestion.agent.utils;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import org.apache.commons.lang3.EnumUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class ProxyUtils {

    public static Proxy.Type parseProxyType(String type) {
        return EnumUtils.getEnumIgnoreCase(Proxy.Type.class, type, Proxy.Type.HTTP);
    }

    public static  Proxy buildProxy(Proxy.Type type, String host, int port) {
        return new Proxy(type, new InetSocketAddress(host, port));
    }

    public static  Authenticator buildProxyAuthenticator(String proxyAuthorizationHeader, String proxyUsername, String proxyPassword) {
        return (route, response) -> response.request().newBuilder()
                .header(proxyAuthorizationHeader, Credentials.basic(proxyUsername, proxyPassword))
                .build();
    }

}
