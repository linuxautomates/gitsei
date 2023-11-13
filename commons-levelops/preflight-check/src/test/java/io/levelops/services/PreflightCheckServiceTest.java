package io.levelops.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.PreflightCheck;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class PreflightCheckServiceTest {

    @Test
    public void name() {
        PreflightCheck successCheck = Mockito.mock(PreflightCheck.class);
        when(successCheck.getIntegrationType()).thenReturn("successInteg");
        when(successCheck.check(anyString(), any(Integration.class), any(Token.class))).thenReturn(
                PreflightCheckResults.builder()
                        .success(true)
                        .build());

        PreflightCheck failCheck = Mockito.mock(PreflightCheck.class);
        when(failCheck.getIntegrationType()).thenReturn("failInteg");
        when(failCheck.check(anyString(), any(Integration.class), any(Token.class))).thenReturn(
                PreflightCheckResults.builder()
                        .success(false)
                        .build());

        PreflightCheck timeoutCheck = Mockito.mock(PreflightCheck.class);
        when(timeoutCheck.getIntegrationType()).thenReturn("timeout");
        when(timeoutCheck.check(anyString(), any(Integration.class), any(Token.class))).thenAnswer(ans -> {
            Thread.sleep(2000);
            return PreflightCheckResults.builder()
                    .success(true)
                    .build();
        });

        PreflightCheckService service = new PreflightCheckService(List.of(successCheck, failCheck, timeoutCheck), 1);


        Integration failInteg = Integration.builder()
                .application("failInteg")
                .build();
        Integration successInteg = Integration.builder()
                .application("successInteg")
                .build();
        Integration unsupportedInteg = Integration.builder()
                .application("unsupportedInteg")
                .build();
        Integration timeoutInteg = Integration.builder()
                .application("timeout")
                .build();
        Token token = Token.builder().build();

        PreflightCheckResults out;
        out = service.check("-", failInteg, token);
        assertThat(out.isSuccess()).isFalse();
        out = service.check("-", successInteg, token);
        assertThat(out.isSuccess()).isTrue();
        out = service.check("-", unsupportedInteg, token);
        assertThat(out.isSuccess()).isTrue();
        out = service.check("-", timeoutInteg, token);
        assertThat(out.isSuccess()).isFalse();
        assertThat(out.getException()).isEqualTo("TimeoutException: ");
    }
}