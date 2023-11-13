package io.levelops.internal_api.services.handlers;

import org.apache.commons.lang3.Validate;

public class LevelOpsLinkUtils {
    private static final String WORKITEM_LINK_FORMAT = "%s/#/admin/workitems/details?workitem=%s";
    private static final String ASSESSMENT_LINK_FORMAT = "%s/#/admin/answer-questionnaire-page?questionnaire=%s&tenant=%s";

    public static String buildWorkItemLink(String appBaseUrl, String vanityId) {
        Validate.notBlank(appBaseUrl, "appBaseUrl cannot be blank!");
        Validate.notBlank(vanityId, "vanityId cannot be blank!");
        return String.format(WORKITEM_LINK_FORMAT, appBaseUrl, vanityId);
    }

    public static String buildQuestionnaireLink(String appBaseUrl, String questionnaireId, String company) {
        Validate.notBlank(appBaseUrl, "appBaseUrl cannot be blank!");
        Validate.notBlank(questionnaireId, "questionnaireId cannot be blank!");
        Validate.notBlank(company, "company cannot be blank!");
        return String.format(ASSESSMENT_LINK_FORMAT, appBaseUrl, questionnaireId, company);
    }
}
