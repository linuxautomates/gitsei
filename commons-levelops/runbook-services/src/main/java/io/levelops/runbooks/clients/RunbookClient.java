package io.levelops.runbooks.clients;

import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.runbooks.models.EvaluateNodeRequest;
import io.levelops.runbooks.models.EvaluateNodeResponse;
import io.levelops.runbooks.models.RunbookClientException;

import java.util.List;
import java.util.Map;

public interface RunbookClient {
    Map<String, String> createRun(final String company, final String runbookId, final String triggerType, final List<RunbookVariable> runbookData) throws RunbookClientException;
    DbListResponse<RunbookRun> listRuns(final String company, final String runbookId, final DefaultListRequest search) throws RunbookClientException;
    Map<String, String> createRunbook(final String company, final Runbook runbook) throws RunbookClientException;
    DbListResponse<Runbook> listRunbooks(final String company, final DefaultListRequest search) throws RunbookClientException;
    Boolean updateRunbookMetadata(final String company, final Runbook runbook) throws RunbookClientException;
    DbListResponse<RunbookRunningNode> listRunningNodes(String company, String runbookId, String runId, DefaultListRequest search) throws RunbookClientException;
    EvaluateNodeResponse evaluateNode(String company, EvaluateNodeRequest evaluateNodeRequest) throws RunbookClientException;
}