package io.levelops.triage.services;

import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;

import java.io.IOException;
import java.util.List;

public interface TriageService {

    String createTriageRule(final String company, final TriageRule rule) throws IOException;

    TriageRule getTriageRule(final String company, final String ruleId) throws IOException;

    String updateTriageRule(final String company, String ruleId, final TriageRule rule) throws IOException;

    DeleteResponse deleteTriageRule(final String company, final String ruleId) throws IOException;

    BulkDeleteResponse bulkDeleteTriageRules(final String company, final List<String> ruleIds) throws IOException;

    DbListResponse<TriageRule> listTriageRules(final String company,
                                                      final DefaultListRequest listRequest) throws IOException;

    DbListResponse<TriageRuleHit> listTriageRuleResults(final String company,
                                                               final DefaultListRequest listRequest) throws IOException;
}