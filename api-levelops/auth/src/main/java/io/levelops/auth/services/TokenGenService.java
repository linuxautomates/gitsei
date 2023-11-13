package io.levelops.auth.services;


import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.utils.JwtTokenUtil;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;

@Log4j2
@SuppressWarnings("unused")
public class TokenGenService {
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthDetailsService authDetailsService;

    public TokenGenService(AuthDetailsService detailsService, JwtTokenUtil tokenUtil) {
        this.jwtTokenUtil = tokenUtil;
        this.authDetailsService = detailsService;
    }

    public String generateToken(String company, ExtendedUser userDetails) {
        return jwtTokenUtil.generateToken(userDetails, company);
    }

    public String generateTokenForMFAEnrollment(String company, ExtendedUser userDetails) {
        return jwtTokenUtil.generateTokenForMFAEnrollment(userDetails, company);
    }

    public String generateTokenForOnboarding(String company, ExtendedUser userDetails) {
        return jwtTokenUtil.generateTokenForOnboarding(userDetails, company);
    }

    public String generateShortTermToken(String company, String username) throws SQLException {
        final ExtendedUser userDetails = authDetailsService.loadUserByUsernameAndOrg(username, company);
        return jwtTokenUtil.generateShortTermToken(userDetails, company);
    }
}
