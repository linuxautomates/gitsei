package io.levelops.ingestion.controllers.generic;

import io.levelops.ingestion.controllers.DataController;

public interface IntegrationController<Q extends IntegrationQuery> extends DataController<Q> {

    String getIntegrationType();
    String getDataType();

}
