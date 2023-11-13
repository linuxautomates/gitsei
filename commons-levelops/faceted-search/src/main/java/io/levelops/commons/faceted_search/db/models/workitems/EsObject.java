package io.levelops.commons.faceted_search.db.models.workitems;

public interface EsObject {
    String generateESId(String company);
    Long getOffset();

    Integer getIntegrationId();
}
