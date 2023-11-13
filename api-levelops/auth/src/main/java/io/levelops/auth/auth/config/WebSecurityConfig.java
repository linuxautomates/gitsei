package io.levelops.auth.auth.config;

import com.google.common.collect.ImmutableList;
import io.levelops.auth.auth.filter.AuthRequestFilter;
import io.levelops.auth.auth.filter.HarnessAuthRequestFilter;
import io.levelops.auth.auth.service.AuthDetailsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Log4j2
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AuthDetailsService authDetailsService;
    private final AuthRequestFilter authRequestFilter;
    private final HarnessAuthRequestFilter harnessAuthRequestFilter;
    @Value("${ALLOWED_CORS_HOSTNAME:https://app.levelops.io}")
    private String ALLOWED_CORS_HOSTNAME;

    @Autowired
    public WebSecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                             AuthDetailsService authDetailsService,
                             AuthRequestFilter authRequestFilter,
                             HarnessAuthRequestFilter harnessAuthRequestFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.authDetailsService = authDetailsService;
        this.authRequestFilter = authRequestFilter;
        this.harnessAuthRequestFilter = harnessAuthRequestFilter;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, PasswordEncoder encoder) {
        auth.authenticationProvider(authProvider(encoder));
    }

    private AuthenticationProvider authProvider(PasswordEncoder encoder) {
        return new CustomAuthenticationProvider(encoder, authDetailsService);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration samlAuthConfiguration = new CorsConfiguration();
        samlAuthConfiguration.setAllowedOrigins(Collections.singletonList("*"));
        samlAuthConfiguration.setAllowedMethods(ImmutableList.of("*"));
        samlAuthConfiguration.setAllowedHeaders(ImmutableList.of("authorization", "content-type", "content-disposition"));
        samlAuthConfiguration.setMaxAge((long) (3600 * 30 * 24));
        CorsConfiguration commonConfiguration = new CorsConfiguration();
        var corsHosts = Arrays.asList(ALLOWED_CORS_HOSTNAME.split(","));
        
        // for (String corsHost:ALLOWED_CORS_HOSTNAME.split(",")) {
        //     corsHosts.add(corsHost);
        // }
        log.info("CORS HOSTS: {}", corsHosts);
        commonConfiguration.setAllowedOrigins(corsHosts);
        commonConfiguration.setAllowedMethods(ImmutableList.of("*"));
        commonConfiguration.setExposedHeaders(ImmutableList.of("content-type", "content-disposition"));
        commonConfiguration.setAllowedHeaders(ImmutableList.of("authorization", "content-type", "content-disposition", "dashboard-id"));
        commonConfiguration.setMaxAge((long) (3600 * 30 * 24));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //The order in which these are set MATTERS! dont ask why. And we need to allow saml auth to come from anywhere.
        source.registerCorsConfiguration("/v1/saml_auth", samlAuthConfiguration);
        source.registerCorsConfiguration("/**", commonConfiguration);
        return source;
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf().disable()
                // dont authenticate this particular request
                .authorizeRequests().antMatchers(
                    "/",
                    "/v1/authenticate",
                    "/v1/saml_auth", 
                    "/v1/change_password",
                    "/v1/forgot_password",
                    "/v1/generate_authn",
                    "/v1/validate/**",
                    "/v1/plugins/**",
                    "/webhooks/slack/**",
                    "/webhooks/github/**",
                    "/v1/admin/tenant")
                .permitAll()
                .antMatchers("/api/**").hasIpAddress("127.0.0.1")
                .antMatchers("/actuator/**").hasIpAddress("127.0.0.1")
                // all other requests need to be authenticated
                .anyRequest().authenticated().and()
                // make sure we use stateless session; session won't be used to
                // store user's state.
                .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .cors();

        // Add a filter to validate the tokens with every request
        httpSecurity.addFilterBefore(authRequestFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(harnessAuthRequestFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(authRequestFilter, HarnessAuthRequestFilter.class);
        httpSecurity.addFilterBefore(new ForwardedHeaderFilter(), AuthRequestFilter.class);
        log.debug("Security configuration applied: {}", httpSecurity);
    }
}
