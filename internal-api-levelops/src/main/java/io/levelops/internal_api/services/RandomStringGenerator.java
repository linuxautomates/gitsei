package io.levelops.internal_api.services;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class RandomStringGenerator {
    public String randomString() {
        return RandomStringUtils.randomAlphanumeric(10);
    }
}
