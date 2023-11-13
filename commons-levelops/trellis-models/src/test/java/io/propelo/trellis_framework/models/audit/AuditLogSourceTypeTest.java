package io.propelo.trellis_framework.models.audit;

import org.junit.Assert;
import org.junit.Test;

public class AuditLogSourceTypeTest {
    @Test
    public void test() {
        for(AuditLogSourceType t : AuditLogSourceType.values()) {
            Assert.assertEquals(t, AuditLogSourceType.fromString(t.toString()));
        }
    }
}