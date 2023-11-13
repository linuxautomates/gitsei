package io.levelops.auth.auth.config;

import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.auth.token.CustomAuthenticationToken;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

@Log4j2
public class CustomAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    private PasswordEncoder passwordEncoder;
    private AuthDetailsService userDetailsService;

    CustomAuthenticationProvider(PasswordEncoder passwordEncoder, AuthDetailsService userDetailsService) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {

        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");
            throw new BadCredentialsException(
                    messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials",
                            "Bad credentials"));
        }

        if(!((ExtendedUser)userDetails).getPasswordAuthEnabled()) {
            throw new BadCredentialsException("Password Auth Disabled for user.");
        }

        String presentedPassword = authentication.getCredentials()
                .toString();

        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            logger.debug("Authentication failed: password does not match stored value");
            throw new BadCredentialsException(
                    messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials",
                            "Bad credentials"));
        }
    }

    @Override
    protected void doAfterPropertiesSet() throws Exception {
        Assert.notNull(this.userDetailsService, "A UserDetailsService must be set");
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        CustomAuthenticationToken auth = (CustomAuthenticationToken) authentication;
        UserDetails loadedUser;

        try {
            loadedUser = this.userDetailsService.loadUserByUsernameAndOrg(auth.getPrincipal()
                    .toString(), auth.getOrg());
        } catch (Exception repositoryProblem) {
            log.error(repositoryProblem);
            throw new InternalAuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
        }

        if (loadedUser == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, "
                    + "which is an interface contract violation");
        }
        return loadedUser;
    }

}