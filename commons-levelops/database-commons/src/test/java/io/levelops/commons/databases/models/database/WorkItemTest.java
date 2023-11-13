package io.levelops.commons.databases.models.database;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

public class WorkItemTest {
    @Test
    public void testTicketType() {
        WorkItem.TicketType ticketType = WorkItem.TicketType.WORK_ITEM;
        Assert.assertEquals("WORK_ITEM", ticketType.toString());
        Assert.assertEquals("WorkItem", ticketType.getDescription());
        for (WorkItem.TicketType t : WorkItem.TicketType.values()) {
            Assert.assertEquals(t, WorkItem.TicketType.fromString(t.toString()));
        }
    }

    @Test
    public void validationTest() {
        WorkItem item = WorkItem.builder().ticketTemplateId("1").build();
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();

        Set<ConstraintViolation<WorkItem>> results = validator.validate(item);
        Assertions.assertThat(results.size()).isEqualTo(1);

        item = WorkItem.builder().ticketTemplateId("bcda46e6-40cb-460b-9728-d299dd41ab00").build();
        results = validator.validate(item);
        Assertions.assertThat(results.size()).isEqualTo(0);

        item = WorkItem.builder().ticketTemplateId(null).build();
        results = validator.validate(item);
        Assertions.assertThat(results.size()).isEqualTo(0);

        item = WorkItem.builder().ticketTemplateId("").build();
        results = validator.validate(item);
        Assertions.assertThat(results.size()).isEqualTo(1);
    }
}