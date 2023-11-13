package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.models.PreflightCheckResults;

public interface PreflightCheck {

    String getIntegrationType();

    PreflightCheckResults check(String tenantId, Integration integration, Token token);

}
