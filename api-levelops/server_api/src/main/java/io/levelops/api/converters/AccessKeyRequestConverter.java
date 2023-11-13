package io.levelops.api.converters;

import io.levelops.api.requests.AccessKeyRequest;
import io.levelops.commons.databases.models.database.AccessKey;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.password.RandomPasswordGenerator;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AccessKeyRequestConverter {
    private final PasswordEncoder passwordEncoder;

    public AccessKeyRequestConverter(@NonNull PasswordEncoder encoder) {
        this.passwordEncoder = encoder;
    }

    //pair returns the user and the randomly generated non-hashed secretkey
    public ImmutablePair<AccessKey, String> convertToAccessKey(AccessKeyRequest source) {
        String secretKey = RandomPasswordGenerator.nextString();
        return ImmutablePair.of(AccessKey.builder()
                .roleType(RoleType.fromString(source.getRole()))
                .description(source.getDescription())
                .name(source.getName())
                .bcryptSecret(passwordEncoder.encode(secretKey))
                .build(), secretKey);
    }
}
