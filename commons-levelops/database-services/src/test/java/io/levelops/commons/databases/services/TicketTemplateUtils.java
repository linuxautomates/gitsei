package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.TicketField;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.models.database.TicketTemplateQuestionnaireTemplateMapping;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TicketTemplateUtils {
    public static void verifyTicketTemplate(TicketTemplate toVerify, TicketTemplate reference, String ticketTemplateId){
        Assert.assertEquals(toVerify.getName(), reference.getName());
        Assert.assertNotNull(toVerify.getCreatedAt());
        Assert.assertEquals(toVerify.getEnabled(), reference.getEnabled());

        Assert.assertEquals(toVerify.getMappings().size(), reference.getMappings().size());
        for (int i=0; i < toVerify.getMappings().size(); ++i){
            TicketTemplateQuestionnaireTemplateMapping am = toVerify.getMappings().get(i);
            TicketTemplateQuestionnaireTemplateMapping em = reference.getMappings().get(i);

            Assert.assertEquals(am.getName(), em.getName());
            Assert.assertEquals(am.getQuestionnaireTemplateId(), em.getQuestionnaireTemplateId());
        }
        Assert.assertNotNull(toVerify.getNotifyBy());
        Assertions.assertThat(toVerify.getNotifyBy()).containsExactlyInAnyOrderEntriesOf(reference.getNotifyBy());
        Assertions.assertThat(toVerify.getMessageTemplateIds()).isEqualTo(reference.getMessageTemplateIds());
        verifyTicketFieldsList(toVerify.getTicketFields(), reference.getTicketFields(), ticketTemplateId);
    }
    public static void verifyTicketFieldsList(List<TicketField> a, List<TicketField> e, String ticketTemplateId){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<String, TicketField> actual = a.stream().collect(Collectors.toMap(x-> x.getField().getKey(), x -> x));
        Map<String, TicketField> expected = e.stream().collect(Collectors.toMap(x-> x.getField().getKey(), x -> x));
        Assert.assertEquals(actual.size(), expected.size());
        for(String key: actual.keySet()){
            verifyTicketField(actual.getOrDefault(key, null), expected.getOrDefault(key,null), ticketTemplateId);
        }
    }
    public static void verifyTicketField(TicketField aField, TicketField eField, String ticketTemplateId){
        Assert.assertEquals(aField.getField().getKey(), eField.getField().getKey());
        Assert.assertEquals(aField.getField().getType(), eField.getField().getType());
        Assert.assertEquals(aField.getField().getOptions(),eField.getField().getOptions());
        Assert.assertEquals(aField.getTicketTemplateId(), ticketTemplateId);
        Assert.assertEquals(aField.getField().getRequired(),eField.getField().getRequired());
        Assert.assertEquals(aField.getField().getDynamicResourceName(),eField.getField().getDynamicResourceName());
        Assert.assertEquals(aField.getField().getValidation(),eField.getField().getValidation());
        Assert.assertEquals(aField.getField().getSearchField(),eField.getField().getSearchField());
        Assert.assertEquals(aField.getField().getDisplayName(),eField.getField().getDisplayName());
        Assert.assertEquals(aField.getField().getDescription(),eField.getField().getDescription());
    }
    public static void verifyTicketTemplatesList(List<TicketTemplate> a, List<TicketTemplate> e, Map<String, String> nameTicketTemplateIds){
        Map<String, TicketTemplate> actual = a.stream()
                .collect(Collectors.toMap(TicketTemplate::getName, x -> x));
        Map<String, TicketTemplate> expected = e.stream()
                .collect(Collectors.toMap(TicketTemplate::getName, x -> x));
        Assert.assertEquals(actual.size(), expected.size());
        for(String key: actual.keySet()){
            verifyTicketTemplate(actual.getOrDefault(key, null), expected.getOrDefault(key,null), nameTicketTemplateIds.getOrDefault(key,null));
        }
    }

    public static TicketTemplate buildTicketTemplate(Integer num, List<String> questionnaireTemplateIds, boolean optionsAreNull){
        String n = num.toString();
        List<TicketField> fields = new ArrayList<>();
        for(int i=0; i <3;i++){
            int index = i+1;
            KvField.KvFieldBuilder bldr = KvField.builder()
                    .key("k" + index).type("t" + index).required(true).dynamicResourceName("d" + index).validation("v" + index).searchField("s" + index).displayName("dn" + index).description("desc" + index);
            if(!optionsAreNull){
                bldr.options(Arrays.asList("k"+index+ "o1", "k"+index+"o2"));
            }
            TicketField field = TicketField.builder().field(bldr.build()).build();
            fields.add(field);
        }
        List<TicketTemplateQuestionnaireTemplateMapping> mappings = new ArrayList<>();
        for(int i=0; i< questionnaireTemplateIds.size(); i++){
            TicketTemplateQuestionnaireTemplateMapping mapping = TicketTemplateQuestionnaireTemplateMapping.builder()
                    .name("mapping name " + n + " " + (i+1))
                    .questionnaireTemplateId(questionnaireTemplateIds.get(i))
                    .build();
            mappings.add(mapping);
        }

        return TicketTemplate.builder()
                .name("tt name " + n)
                .description("test issue")
                .mappings(mappings)
                .ticketFields(fields)
                .notifyBy(Map.of(EventType.ALL, List.of("email")))
                .messageTemplateIds(List.of("1", "2"))
                .enabled(true)
                .build();
    }
    public static TicketTemplate buildTicketTemplate(Integer num, List<String> questionnaireTemplateIds){
        return buildTicketTemplate(num, questionnaireTemplateIds, false);
    }

    public static TicketTemplate modifyTicketTemplate(Integer num, TicketTemplate t){
        String n = num.toString();
        List<TicketTemplateQuestionnaireTemplateMapping> mappings = new ArrayList<>();
        for(int i =0; i < t.getMappings().size(); i++){
            TicketTemplateQuestionnaireTemplateMapping mapping = t.getMappings().get(i).toBuilder()
                    .name("mapping name " + n + " " + (i+1))
                    .build();
            mappings.add(mapping);
        }
        return t.toBuilder().name("tt name " + n).clearMappings().mappings(mappings).enabled(false).build();
    }

}
