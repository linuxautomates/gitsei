package io.levelops.internal_api.converters;

import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.password.RandomPasswordGenerator;
import io.levelops.users.requests.ModifyUserRequest;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ModifyUserRequestToUserConverter {
    private Long resetTokenExpirySeconds;
    private PasswordEncoder passwordEncoder;

    public ModifyUserRequestToUserConverter(@NonNull PasswordEncoder encoder,
                                            Long resetExpiry) {
        this.passwordEncoder = encoder;
        this.resetTokenExpirySeconds = resetExpiry;
    }

    //pair returns the user and the randomly generated non-hashed password reset token
    public ImmutablePair<User, String> convertNewUserRequest(ModifyUserRequest source) {
        String resetToken = RandomPasswordGenerator.nextString();
        return ImmutablePair.of(User.builder()
                .email(source.getEmail())
                .firstName(source.getFirstName())
                .userType(RoleType.fromString(source.getUserType()))
                .lastName(source.getLastName())
                .samlAuthEnabled(source.getSamlAuthEnabled())
                .passwordAuthEnabled(source.getPasswordAuthEnabled())
                .bcryptPassword(passwordEncoder.encode(RandomPasswordGenerator.nextString()))
                .passwordResetDetails(new User.PasswordReset(passwordEncoder.encode(resetToken),
                        System.currentTimeMillis() / 1000 + resetTokenExpirySeconds))
                .mfaEnabled(source.getMfaEnabled() != null ? source.getMfaEnabled() : false)
                .metadata(source.getMetadata())
                .mfaEnrollmentEndAt(source.getMfaEnrollmentWindowExpiryInstant())
                .mfaResetAt(source.getMfaResetAtInstant())
                        .managedOURefIds(source.getManagedOURefIds())
                .build(), resetToken);
    }

    public User convertUpdateRequest(ModifyUserRequest source, String userId) {
        return User.builder()
                .email(source.getEmail())
                .firstName(source.getFirstName())
                .id(userId)
                .userType(RoleType.fromString(source.getUserType()))
                .samlAuthEnabled(source.getSamlAuthEnabled())
                .passwordAuthEnabled(source.getPasswordAuthEnabled())
                .lastName(source.getLastName())
                .mfaEnabled(source.getMfaEnabled())
                .metadata(source.getMetadata())
                .mfaEnrollmentEndAt(source.getMfaEnrollmentWindowExpiryInstant())
                .mfaResetAt(source.getMfaResetAtInstant())
                .managedOURefIds(source.getManagedOURefIds())
                .build();
    }
}
