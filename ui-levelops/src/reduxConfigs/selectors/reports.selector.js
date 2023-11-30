import { createSelector } from "reselect";
import { get } from "lodash";

const reportsRestState = state => {
  return {
    bounce_report: { ...state.restapiReducer.bounce_report },
    hops_report: { ...state.restapiReducer.hops_report },
    resolution_time_report: { ...state.restapiReducer.resolution_time_report },
    response_time_report: { ...state.restapiReducer.response_time_report },
    tickets_report: { ...state.restapiReducer.tickets_report },
    hygiene_report: { ...state.restapiReducer.hygiene_report },
    product_aggs: { ...state.restapiReducer.product_aggs },
    github_prs_report: { ...state.restapiReducer.github_prs_report },
    github_commits_report: { ...state.restapiReducer.github_commits_report },
    jobs_count_report: { ...state.restapiReducer.jobs_count_report },
    jobs_commits_lead_report: { ...state.restapiReducer.jobs_commits_lead_report },
    jobs_change_volume_report: { ...state.restapiReducer.jobs_change_volume_report },
    jobs_duration_report: { ...state.restapiReducer.jobs_duration_report },
    plugin_aggs: { ...state.restapiReducer.plugin_aggs },
    jenkins_job_config_change_report: { ...state.restapiReducer.jenkins_job_config_change_report },
    zendesk_bounce_report: { ...state.restapiReducer.zendesk_bounce_report },
    zendesk_hops_report: { ...state.restapiReducer.zendesk_hops_report },
    zendesk_response_time_report: { ...state.restapiReducer.zendesk_response_time_report },
    zendesk_resolution_time_report: { ...state.restapiReducer.zendesk_resolution_time_report },
    zendesk_tickets_report: { ...state.restapiReducer.zendesk_tickets_report },
    zendesk_hygiene_report: { ...state.restapiReducer.zendesk_hygiene_report },
    zendesk_reopens_report: { ...state.restapiReducer.zendesk_reopens_report },
    zendesk_replies_report: { ...state.restapiReducer.zendesk_replies_report },
    zendesk_agent_wait_time_report: { ...state.restapiReducer.zendesk_agent_wait_time_report },
    zendesk_requester_wait_time_report: { ...state.restapiReducer.zendesk_requester_wait_time_report },
    scm_files_report: { ...state.restapiReducer.scm_files_report },
    scm_issues_report: { ...state.restapiReducer.scm_issues_report },
    scm_issues_first_response_report: { ...state.restapiReducer.scm_issues_first_response_report },
    scm_prs_merge_trend: { ...state.restapiReducer.scm_prs_merge_trend },
    scm_prs_first_review_trend: { ...state.restapiReducer.scm_prs_first_review_trend },
    scm_prs_first_review_to_merge_trend: { ...state.restapiReducer.scm_prs_first_review_to_merge_trend },
    scm_jira_files_report: { ...state.restapiReducer.scm_jira_files_report },
    salesforce_bounce_report: { ...state.restapiReducer.salesforce_bounce_report },
    salesforce_hops_report: { ...state.restapiReducer.salesforce_hops_report },
    salesforce_response_time_report: { ...state.restapiReducer.salesforce_response_time_report },
    salesforce_resolution_time_report: { ...state.restapiReducer.salesforce_resolution_time_report },
    salesforce_tickets_report: { ...state.restapiReducer.salesforce_tickets_report },
    salesforce_hygiene_report: { ...state.restapiReducer.salesforce_hygiene_report }
  };
};

export const exactReportsState = (state, apiCalls) => {
  return (apiCalls || []).reduce((acc, obj) => {
    const uri = obj.apiName;
    const method = obj.apiMethod;
    const id = obj.id;
    if (obj.id.endsWith("-preview") && !get(state.restapiReducer, [uri, method, id], undefined)) {
      const widgetId = id.replace("-preview", "");
      const widgetState = get(state.restapiReducer, [uri, method, widgetId], { loading: true, error: false });
      acc[uri] = {
        ...get(acc, [uri], {}),
        [method]: {
          ...get(acc, [uri, method], {}),
          [id]: widgetState,
          [widgetId]: widgetState
        }
      };
    } else {
      const restState = get(state.restapiReducer, [uri, method, id], { loading: true, error: false });
      acc[uri] = {
        ...get(acc, [uri], {}),
        [method]: {
          ...get(acc, [uri, method], {}),
          [id]: restState
        }
      };
    }
    return acc;
  }, {});
};

export const exactIssuesState = (state, ids) => {
  let issues = {};
  const uri = "jira_tickets";
  const method = "list";
  ids.forEach(id => {
    const restState = get(state.restapiReducer, [uri, method, id], { loading: true, error: false });
    issues[id] = restState;
  });
  return issues;
};

export const getReportsSelector = createSelector(reportsRestState, data => {
  return data;
});

export const exactReportsSelector = createSelector(exactReportsState, data => {
  return data;
});
