package io.levelops.integrations.helix_swarm.client;

import io.levelops.commons.client.ClientConstants;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@Log4j2
public class HelixSwarmTokenInterceptor implements Interceptor {

    private final HelixCoreTicketProvider ticketProvider;

    public HelixSwarmTokenInterceptor(HelixCoreTicketProvider ticketProvider) {
        this.ticketProvider = ticketProvider;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        var token = ticketProvider.getTicket();
        if (token == null) {
            return chain.proceed(chain.request());
        }
        Request request = chain.request().newBuilder()
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BASIC_ + token)
                .build();
        Response response = chain.proceed(request);
        if (response.code() == 401) {
            response.close();
            synchronized (this) {
                String newToken = ticketProvider.getTicket();
                Request newRequest;
                if (!newToken.equals(token)) {
                    newRequest = response.request()
                            .newBuilder()
                            .header(ClientConstants.AUTHORIZATION, ClientConstants.BASIC_ + newToken)
                            .build();
                } else {
                    String updatedToken;
                    updatedToken = ticketProvider.refreshTicket();
                    if (updatedToken == null) {
                        return response;
                    }
                    newRequest = response.request()
                            .newBuilder()
                            .header(ClientConstants.AUTHORIZATION, ClientConstants.BASIC_ + updatedToken)
                            .build();
                }
                return chain.proceed(newRequest);
            }
        }
        return response;
    }
}
