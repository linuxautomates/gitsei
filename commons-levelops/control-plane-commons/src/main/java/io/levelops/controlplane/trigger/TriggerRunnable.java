package io.levelops.controlplane.trigger;

import io.levelops.controlplane.models.DbTrigger;

public interface TriggerRunnable {

    void run(DbTrigger trigger) throws Exception;

    String getTriggerType();

}
