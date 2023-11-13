package io.levelops.auth.auth.authobject;

import io.levelops.commons.databases.models.database.access.RoleType;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class ExtendedUser extends User {
    /**
     *
     */
    private static final long serialVersionUID = 1062795485770156255L;
    private final String firstName;
    private final String lastName;
    private final String userType;
    private final Boolean passwordAuthEnabled;
    private final Boolean samlAuthEnabled;
    private final Boolean mfaRequired;
    private final Boolean mfaEnrollment;
    private final Boolean mfaEnabled;
    private final Map<String, List<String>> scopes;
    private final Set<String> entitlements;
    private final String license;

    public ExtendedUser(String username, String password, Boolean passwordAuthEnabled,
                        Boolean samlAuthEnabled, RoleType userType, String firstName,
                        String lastName, Collection<? extends GrantedAuthority> authorities,
                        Boolean mfaRequired, Boolean mfaEnrollment, Boolean mfaEnabled, Map<String, List<String>> scopes,
                        Set<String> entitlements,
                        String license) {
        super(username, password, authorities);
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType != null ? userType.toString() : "";
        this.samlAuthEnabled = samlAuthEnabled;
        this.passwordAuthEnabled = passwordAuthEnabled;
        this.mfaEnabled = mfaEnabled;
        this.mfaRequired = mfaRequired;
        this.mfaEnrollment = mfaEnrollment;
        this.scopes = scopes;
        this.entitlements = entitlements;
        this.license = license;
    }

    public ExtendedUser(String username, String password, Boolean passwordAuthEnabled,
                        Boolean samlAuthEnabled, RoleType userType, String firstName,
                        String lastName, Collection<? extends GrantedAuthority> authorities,
                        Boolean mfaRequired, Boolean mfaEnrollment, Boolean mfaEnabled, Map<String, List<String>> scopes) {
        super(username, password, authorities);
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType.toString();
        this.samlAuthEnabled = samlAuthEnabled;
        this.passwordAuthEnabled = passwordAuthEnabled;
        this.mfaEnabled = mfaEnabled;
        this.mfaRequired = mfaRequired;
        this.mfaEnrollment = mfaEnrollment;
        this.scopes = scopes;
        this.entitlements = Set.of("ALL_FEATURES");
        this.license = "limited_trial";
    }

    public static ExtendedUserBuilder ExtendedUserBuilder(){
        return new ExtendedUserBuilder();
    }

    public ExtendedUserBuilder toBuilder(){
        return new ExtendedUserBuilder(this);
    }

    @Accessors(fluent = true)
    @Data
    public static class ExtendedUserBuilder {
        private String username;
        private String password;
        private Collection<? extends GrantedAuthority> authorities;
        private String firstName;
        private String lastName;
        private RoleType userType;
        private Boolean passwordAuthEnabled;
        private Boolean samlAuthEnabled;
        private Boolean mfaRequired;
        private Boolean mfaEnrollment;
        private Boolean mfaEnabled;
        private Map<String, List<String>> scopes;
        private Set<String> entitlements;
        private String license;

        private ExtendedUserBuilder(){
        }

        private ExtendedUserBuilder(final ExtendedUser seed){
            this.username = seed.getUsername();
            this.password = seed.getPassword();
            this.authorities = seed.getAuthorities();
            this.firstName = seed.firstName;
            this.lastName = seed.lastName;
            this.userType = RoleType.fromString(seed.getUserType());
            this.passwordAuthEnabled = seed.passwordAuthEnabled;
            this.samlAuthEnabled = seed.samlAuthEnabled;
            this.mfaEnabled = seed.mfaEnabled;
            this.mfaRequired = seed.mfaRequired;
            this.mfaEnrollment = seed.mfaEnrollment;
            this.scopes = seed.scopes;
            this.entitlements = seed.entitlements;
            this.license = seed.license;
        }

        public ExtendedUser build(){
            return new ExtendedUser(this.username, this.password, this.passwordAuthEnabled, this.samlAuthEnabled,
                                    this.userType, this.firstName, this.lastName, this.authorities, this.mfaRequired,
                                    this.mfaEnrollment, this.mfaEnabled, this.scopes, this.entitlements, this.license);
        }

    }
}
