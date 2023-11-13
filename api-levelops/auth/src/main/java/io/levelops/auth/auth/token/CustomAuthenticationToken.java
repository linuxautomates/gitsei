package io.levelops.auth.auth.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private String org;
    private String otp;

    public CustomAuthenticationToken(Object principal, Object credentials, String org) {
        super(principal, credentials);
        super.setAuthenticated(false);
        this.org = org;
        this.otp = null;
    }

    public CustomAuthenticationToken(Object principal, Object credentials, String org, String otp) {
        super(principal, credentials);
        super.setAuthenticated(false);
        this.org = org;
        this.otp = otp;
    }

    public CustomAuthenticationToken(Object principal, Object credentials, String org,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        super.setAuthenticated(true); // must use super, as we override
        this.org = org;
        this.otp = null;
    }

    public CustomAuthenticationToken(Object principal, Object credentials, String org, String otp,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        super.setAuthenticated(true); // must use super, as we override
        this.org = org;
        this.otp = otp;
    }

    public String getOrg() {
        return this.org;
    }

    public String getOtp(){
        return this.otp;
    }
}