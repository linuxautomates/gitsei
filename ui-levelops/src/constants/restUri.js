import envConfig from "env-config";

export const getBaseAPIUrl = () =>
  !window.isStandaloneApp ? window.location.origin.concat("/sei/api") : envConfig.get("API_URL");

export const buildApiUrl = url => {
  if (!window.isStandaloneApp && window.getApiBaseUrl) {
    const modifiedUrl = url.startsWith("/") ? url : `/${url}`;
    return window.getApiBaseUrl(`/sei/api${modifiedUrl}`);
  }
  return envConfig.get("API_URL").concat(url);
};
export const VERSION = envConfig.get("API_VERSION") || "v1";

// authentication endpoints
export const LOGIN = `/${VERSION}/authenticate`;
export const REFRESH = `/${VERSION}/refresh`;

// Sign-in
export const VALIDATE_EMAIL = `/${VERSION}/validate/email`;
export const VALIDATE_COMPANY = `/${VERSION}/validate/company`;

// external integration endpoints
export const INTEGRATION = `/${VERSION}/integration`;
export const INTEGRATIONS = `/${VERSION}/integrations`;

// dashboard endpoints
export const DASHBOARD = `/${VERSION}/dashboard`;
export const DASHBOARDS = `/${VERSION}/dashboards`;
export const DASHBOARD_REPORTS = `/${VERSION}/dashboard_reports`;

// analytics endpoints
export const QUERY = `/${VERSION}/query`;

// notification endpoints
export const NOTIFICATIONS = `/${VERSION}/notifications`;

// user CRUD endpoints
export const USERS = `/${VERSION}/users`;
export const USER = `/${VERSION}/user`;
export const TRELLIS_SCOPES = `${USERS}/trellis/`;

// rbac CRUD endpoints
export const RBAC = `/${VERSION}/rbac`;
export const RBACS = `/${VERSION}/rbacs`;

// sub-org and team endpoints

export const ORGS = `/${VERSION}/organizations`;
export const ORG = `/${VERSION}/organization`;
export const TEAMS = `/${VERSION}/teams`;
export const TEAM = `/${VERSION}/team`;

// team mappings for integrations

export const TEAM_MAPPINGS = `${VERSION}/mappings`;

// saml config

export const SAML_CONFIG = `${VERSION}/samlconfig`;

//  user profile

export const USER_PROFILE = `${VERSION}/users/me`;

// repos

export const REPOS = `${VERSION}/repos`;

// releases

export const RELEASES = `${VERSION}/releases`;

// deployments

export const DEPLOYMENTS = `${VERSION}/deployments`;

// technologies

export const TECHNOLOGIES = `${VERSION}/technologies`;

// filechanges

export const FILECHANGES = `${VERSION}/filechanges`;

// policy

export const POLICIES = `${VERSION}/policies`;

// bps

export const BPS = `${VERSION}/bestpractices`;

// tools

export const TOOLS = `${VERSION}/tools`;

// questionnaires

export const QUESTIONNAIRES = `${VERSION}/qtemplates`;
export const QUESTIONNAIRES_NOTIFICATION = `${VERSION}/questionnaires_notifications`;

// questions

export const SECTIONS = `${VERSION}/sections`;

// tags

export const TAGS = `${VERSION}/tags`;

// alerts

export const ALERTS = `${VERSION}/alerts`;

// resources

export const RESOURCES = `${VERSION}/resources`;

// accounts

export const ACCOUNTS = `${VERSION}/accounts`;

// configs

export const CONFIGS = `${VERSION}/configs`;

// developers

export const DEVELOPERS = `${VERSION}/developers`;

// quiz

export const QUIZ = `${VERSION}/questionnaires`;

// fileupload

export const FILE_UPLOAD = `${VERSION}/fileupload`;

// workitem

export const WORK_ITEM = `${VERSION}/workitems`;

// notes

export const NOTES = `${VERSION}/notes`;

// send communucations

export const COMMS = `${VERSION}/comms`;

// comm templates

export const CTEMPLATE = `${VERSION}/message_templates`;

// password reset request

export const PASSWORD_RESET = `${VERSION}/forgot_password`;

export const PASSWORD_CHANGE = `${VERSION}/change_password`;

// checklists

export const CHECKLISTS = `${VERSION}/checklists`;

// products

export const PRODUCTS = `${VERSION}/products`;

// gitrepos

export const GITREPOS = `${VERSION}/repositories`;

// jira projects

export const JIRAPROJECTS = `${VERSION}/jiraprojects`;

// activity logs

export const ACTIVITYLOGS = `${VERSION}/activitylogs`;

// fields

export const FIELDS = `${VERSION}/fields`;

// repositories

export const REPOSITORIES = `${VERSION}/repositories`;

// stages

export const STAGES = `${VERSION}/stages`;

// mappings

export const MAPPINGS = `${VERSION}/mappings`;

// violation logs

// signature logs

export const SIGNATURE_LOGS = `${VERSION}/signature_logs`;

export const EVENT_LOGS = `${VERSION}/event_logs`;

// signatures

export const SIGNATURES = `${VERSION}/signatures`;

// workflows

export const WORKFLOWS = `${VERSION}/workflows`;

export const METRICS = `${VERSION}/metrics`;

// apikeys

export const APIKEYS = `${VERSION}/apikeys`;

// plugins

export const PLUGINS = `${VERSION}/plugins`;

export const PLUGINS_TRIGGER = `${VERSION}/plugins_trigger`;

// plugin results

export const PLUGIN_RESULTS = `${VERSION}/plugins/results`;

export const PLUGIN_LABELS = `${VERSION}/plugins/results/labels`;

// plugin aggs

export const PLUGIN_AGGS = `${VERSION}/plugin_aggs`;

// smart ticket issue template

export const SMART_TICKET_TEMPLATES = `${VERSION}/ticket_templates`;

export const CUSTOM_FIELDS = `${VERSION}/custom-fields`;

export const AUTOMATION_RULES = `${VERSION}/automation_rules`;

export const OBJECTS_ROUTE = "objects";
export const OBJECTS = `${VERSION}/${OBJECTS_ROUTE}`;

// Propels

export const PROPEL_NODE_TEMPLATES = `${VERSION}/playbooks/node_templates`;

export const PROPEL_NODE_CATEGORIES = `${VERSION}/playbooks/node_templates/categories`;

export const PROPEL_TRIGGER_TEMPLATES = `${VERSION}/playbooks/triggers/schemas`;

export const PROPEL = `${VERSION}/playbooks`;

export const PROPEL_TRIGGER_EVENTS = `${VERSION}/events/types`;

export const PROPEL_RUNS = `${VERSION}/playbooks/runs`;
export const PROPEL_RUN = `${VERSION}/playbooks/run`;

export const PROPEL_REPORTS = `${VERSION}/playbooks/reports`;

export const PROPEL_NODES_EVALUATE = `${VERSION}/playbooks/nodes/evaluate`;

// reports

export const REPORTS = `${VERSION}/reports`;

// product aggs

export const PRODUCT_AGGS = `${VERSION}/product_aggs`;

export const STATES = `${VERSION}/states`;

export const CONTENT_SCHEMA = `${VERSION}/content/schema`;

// configure-dashboard
export const CONFIGURE_DASHBOARD = `${VERSION}/configure-dashboard`;

export const FILTER_VALUES = `/values`;

//jira-issues
export const JIRA_ISSUES = `${VERSION}/jira_issues`;
export const JIRA_FIELDS = `${VERSION}/jira_fields`;

// Microsoft
export const MICROSOFT_ISSUES_URI = `mtmt/issues`;
export const MICROSOFT_THREAT = `${VERSION}/${MICROSOFT_ISSUES_URI}`;
export const MICROSOFT_THREAT_REPORT = `${MICROSOFT_THREAT}/aggs`;
export const MICROSOFT_THREAT_LIST = `${MICROSOFT_THREAT}/list`;
export const MICROSOFT_ISSUES_FILTER_VALUES = "microsoft_issues_filter_values";
export const MICROSOFT_ISSUES_REPORT = "microsoft_threat_modeling_issues_report";
export const MICROSOFT_ISSUES = "microsoft_issues";

// cicd combined report

export const COMBINED = `${VERSION}/cicd_scm`;

//reports
export const BOUNCE_REPORT = `${JIRA_ISSUES}/bounce_report`;
export const HOPS_REPORT = `${JIRA_ISSUES}/hops_report`;
export const RESPONSE_TIME_REPORT = `${JIRA_ISSUES}/response_time_report`;
export const RESOLUTION_TIME_REPORT = `${JIRA_ISSUES}/resolution_time_report`;
export const TICKETS_REPORT = `${JIRA_ISSUES}/tickets_report`;
export const HYGIENE_REPORT = `${JIRA_ISSUES}/hygiene_report`;
export const ASSIGNEE_TIME_REPORT = `${JIRA_ISSUES}/assignee_time_report`;
export const FIRST_ASSIGNEE_REPORT = `${JIRA_ISSUES}/first_assignee_report`;
export const JOB_COUNT_REPORT = `${COMBINED}/job_counts`;
export const JOB_COUNT_BY_CICD_REPORT = `${COMBINED}/job_counts_by_cicd_user`;
export const JOB_DURATIONS_REPORT = `${COMBINED}/job_durations`;
export const JOB_DURATIONS_BY_CICD_REPORT = `${COMBINED}/job_durations_by_cicd_user`;
export const JOB_COMMITS_LEAD_TIME_REPORT = `${COMBINED}/jobs_commit_lead_time`;
export const JOB_COMMITS_LEAD_TIME_CICD_REPORT = `${COMBINED}/jobs_commit_lead_time_by_scm_user_id`;
export const JOB_CHANGE_VOLUME_REPORT = `${COMBINED}/jobs_change_volumes`;
export const BACKLOG_REPORT = `${JIRA_ISSUES}/age_report`;
export const TIME_ACROSS_STAGES_REPORT = `${JIRA_ISSUES}/stage_times_report`;
export const STORY_POINT_REPORT = `${JIRA_ISSUES}/story_point_report`;
export const SPRINT_REPORT = `${JIRA_ISSUES}/sprint_metrics_report`;
export const TEAM_ALLOCATION_REPORT = `${JIRA_ISSUES}/assignee_allocation_report`;
export const JIRA_SRPINTS = `${JIRA_ISSUES}/sprints`;
export const EPIC_PRIORITY_TREND_REPORT = `${JIRA_ISSUES}/priority_trend_report`;
export const JIRA_STAGE_BOUNCE_REPORT = `${JIRA_ISSUES}/stage_bounce_report`;
export const JIRA_LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT = `${JIRA_ISSUES}/velocity_stage_times_report`;
export const JIRA_RELEASE_TABLE_REPORT = `${JIRA_ISSUES}/release_table_report`;
export const JIRA_RELEASE_TABLE_REPORT_DRILLDOWN = `${JIRA_RELEASE_TABLE_REPORT}/list`;
// combined aggs query end points

export const CICD_USERS = `${COMBINED}/cicd_user_ids`;
export const JOB_STATUSES = `${COMBINED}/job_statuses`;
export const SCM_USERS = `${COMBINED}/scm_user_ids`;
export const CICD_JOB_NAMES = `${COMBINED}/cicd_job_names`;

export const JIRA_PRIORITIES = `${JIRA_ISSUES}/priorities`;

//jira filter values
export const JIRA_FILTER_VALUES = `${JIRA_ISSUES}/values`;
export const JIRA_CUSTOM_FILTER_VALUES = `${JIRA_ISSUES}/custom_field/values`;
export const JIRA_CUSTOM_CONFIG = `${VERSION}/integration_configs`;

// combined filter values

export const CICD_FILTER_VALUES = `${COMBINED}/values`;
export const CICD_LIST = `${COMBINED}`;

export const CICD_SCM_JOB_RUNS = `${VERSION}/cicd/job_runs`;
export const CICD_SCM_JOB_AGG = `${VERSION}/cicd/jobs_agg`;
export const CICD_SCM_JOB_STAGES = `${VERSION}/cicd/jobs`;
export const CICD_SCM_JOB_RUN_TESTS = `${CICD_SCM_JOB_RUNS}/tests`;
export const CICD_SCM_JOB_RUN_TESTS_FILTER_VALUES = `${CICD_SCM_JOB_RUN_TESTS}/values`;
export const CICD_SCM_JOB_RUN_TESTS_REPORT = `${CICD_SCM_JOB_RUN_TESTS}/tests_report`;
export const CICD_SCM_JOB_RUNS_TESTS_DURATION_REPORT = `${CICD_SCM_JOB_RUN_TESTS}/duration_report`;

//github
export const GITHUB_SCM = `${VERSION}/scm`;

//file extentions
export const GITHUB_SCM_FILE_EXT = `${GITHUB_SCM}/filetypes`;

//github repos
export const GITHUB_SCM_REPOS = `${GITHUB_SCM}/repos`;
export const SCM_REPO_NAMES = `${GITHUB_SCM}/repo_names`;
//github committers
export const GITHUB_SCM_COMMITTERS = `${GITHUB_SCM}/committers`;

// SCM DORA
export const SCM_DORA_LEAD_TIME = `${GITHUB_SCM}/dora/lead_time`;
export const SCM_DORA_DEPLOYMENT_FREQUENCY = `${GITHUB_SCM}/dora/deployment_frequency`;

//github prs
export const GITHUB_PRS = `${GITHUB_SCM}/prs`;
export const GITHUB_PRS_REPORT = `${GITHUB_PRS}/aggregate`;
export const GITHUB_PRS_FILTER_VALUES = `${GITHUB_PRS}/values`;
export const SCM_PRS_AUTHOR_RESPONSE_TIME = `${GITHUB_PRS}/author_response_time_report`;
export const SCM_PRS_REVIEWER_RESPONSE_TIME = `${GITHUB_PRS}/reviewer_response_time_report`;

//github commit
export const GITHUB_COMMITS = `${GITHUB_SCM}/commits`;
export const GITHUB_COMMITS_REPORT = `${GITHUB_COMMITS}/aggregate`;
export const GITHUB_COMMITS_FILTER_VALUES = `${GITHUB_COMMITS}/values`;
export const GITHUB_CODING_DAY = `${GITHUB_COMMITS}/coding_days_report`;
export const GITHUB_COMMITS_PER_CODING_DAY = `${GITHUB_COMMITS}/commits_per_coding_day`;

export const SCM_REWORK_REPORT = `${GITHUB_SCM}/rework_report`;

//github cards
export const GITHUB_CARDS = `${VERSION}/github/cards`;
export const GITHUB_TIME_ACROSS_STAGES = `${GITHUB_CARDS}/stage_times_report`;

//jenkins only
export const CICD = `${VERSION}/cicd`;
export const CICD_JOB_CONFIG_CHANGE = `${CICD}/job_config_changes`;
export const CICD_PIPELINES = `${CICD}/pipelines`;
export const PIPELINE_JOB_COUNTS_REPORT = `${CICD_PIPELINES}/job_counts`;
export const PIPELINE_JOB_DURATIONS_REPORT = `${CICD_PIPELINES}/job_durations`;
export const JENKINS_PIPELINE_JOB_RUNS = `${CICD_PIPELINES}/job_runs`;
export const JENKINS_PIPELINE_JOBS_RUNS_LIST = `${JENKINS_PIPELINE_JOB_RUNS}/list`;
export const JENKINS_PIPELINE_JOBS_STAGES_LIST = `${CICD}/jobs/steps`;
export const JENKINS_PIPELINE_JOBS_RUNS_TRIAGE = `${CICD_PIPELINES}/triage_job_runs`;
export const JENKINS_PIPELINE_JOBS_RUNS_TRIAGE_LIST = `${JENKINS_PIPELINE_JOBS_RUNS_TRIAGE}/list`;
export const CICD_JOBS = `${CICD}/jobs`;

//jenkins only filter values
export const JENKINS_FILTER_VALUES = `${CICD_JOB_CONFIG_CHANGE}/values`;
export const JENKINS_PIPELINE_FILTER_VALUES = `${JENKINS_PIPELINE_JOB_RUNS}/values`;

//zendesk
export const ZENDESK = `${VERSION}/zendesk_tickets`;
export const ZENDESK_FILTER_VALUES = `${ZENDESK}/values`;
export const ZENDESK_BOUNCE_REPORT = `${ZENDESK}/bounce_report`;
export const ZENDESK_HOPS_REPORT = `${ZENDESK}/hops_report`;
export const ZENDESK_RESPONSE_TIME_REPORT = `${ZENDESK}/response_time_report`;
export const ZENDESK_RESOLUTION_TIME_REPORT = `${ZENDESK}/resolution_time_report`;
export const ZENDESK_TICKETS_REPORT = `${ZENDESK}/tickets_report`;
export const ZENDESK_HYGIENE_REPORT = `${ZENDESK}/hygiene_report`;
export const ZENDESK_AGENT_WAIT_TIME_REPORT = `${ZENDESK}/agent_wait_time_report`;
export const ZENDESK_REQUESTER_WAIT_TIME_REPORT = `${ZENDESK}/requester_wait_time_report`;
export const ZENDESK_REOPENS_REPORT = `${ZENDESK}/reopens_report`;
export const ZENDESK_REPLIES_REPORT = `${ZENDESK}/replies_report`;
export const JENKINS_JOBS_FILTER_VALUES = `${CICD}/job_runs/values`;
export const ZENDESK_FIELDS = `${VERSION}/zendesk_fields`;
export const ZENDESK_CUSTOM_FILTER_VALUES = `${ZENDESK}/custom_field/values`;

//SCM FILE METRICS
export const SCM_FILES = `${GITHUB_SCM}/files`;
export const SCM_FILES_FILTER_VALUES = `${SCM_FILES}/values`;

//SCM ISSUES
export const SCM_ISSUES = `${GITHUB_SCM}/issues`;
export const SCM_ISSUES_FILTER_VALUES = `${SCM_ISSUES}/values`;
export const SCM_ISSUE_REPORT = `${SCM_ISSUES}/aggregate`;
export const SCM_ISSUE_FIRST_RESPONSE = `${SCM_ISSUES}/first_response_time`;
export const SCM_RESOLUTION_TIME_REPORT = `${SCM_ISSUES}/resolution_time_report`;

//SCM PRs METRICS
export const SCM_PRS_MERGE_TREND = `${GITHUB_PRS}/merge_trend`;
export const SCM_PRS_FIRST_REVIEW_TREND = `${GITHUB_PRS}/first_review_trend`;
export const SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND = `${GITHUB_PRS}/first_review_to_merge_trend`;

//SCM JIRA FILES
export const SCM_JIRA = `${VERSION}/scm_jira`;
export const SCM_JIRA_FILES = `${SCM_JIRA}/files`;

//SERVICES
export const SERVICES_REPORT = `${VERSION}/services`;
export const SERVICES__REPORT_AGGREGATES = `${SERVICES_REPORT}/aggregate`;
export const SERVICES__REPORT_AGGREGATES_FILTERS_VALUES = `${SERVICES__REPORT_AGGREGATES}/values`;

//SALESFORCE
export const SALESFORCE = `${VERSION}/salesforce_cases`;
export const SALESFORCE_FILTER_VALUES = `${SALESFORCE}/values`;
export const SALESFORCE_BOUNCE_REPORT = `${SALESFORCE}/bounce_report`;
export const SALESFORCE_HOPS_REPORT = `${SALESFORCE}/hops_report`;
export const SALESFORCE_RESPONSE_TIME_REPORT = `${SALESFORCE}/response_time_report`;
export const SALESFORCE_RESOLUTION_TIME_REPORT = `${SALESFORCE}/resolution_time_report`;
export const SALESFORCE_TICKETS_REPORT = `${SALESFORCE}/cases_report`;
export const SALESFORCE_HYGIENE_REPORT = `${SALESFORCE}/hygiene_report`;

export const ASSESSMENTS_DOWNLOAD = `${VERSION}/assessments/download`;

//Configuration Tables
export const CONFIG_TABLES = `${VERSION}/config-tables`;
export const JIRA_ZENDESK_AGGS = `${VERSION}/jira_zendesk/agg`;
export const JIRA_ZENDESK_AGGS_LIST_JIRA = `${VERSION}/jira_zendesk/list_jira`;
export const JIRA_ZENDESK_AGGS_LIST_ZENDESK = `${VERSION}/jira_zendesk/list_zendesk`;
export const JIRA_ZENDESK_AGGS_LIST_COMMIT = `${VERSION}/jira_zendesk/list_commit`;
export const JIRA_ZENDESK_ESCALATION_TIME_REPORT = `${VERSION}/jira_zendesk/escalation_time_report`;
export const JIRA_ZENDESK_FILES = `${VERSION}/jira_zendesk/files`;
export const JIRA_ZENDESK_FILES_REPORT = `${VERSION}/jira_zendesk/files_report`;
export const JIRA_ZENDESK_RESOLVED_TICKETS_TREND = `${VERSION}/jira_zendesk/resolved_tickets_trend`;

export const JIRA_SALESFORCE_AGGS = `${VERSION}/jira_salesforce/agg`;
export const JIRA_SALESFORCE_AGGS_LIST_JIRA = `${VERSION}/jira_salesforce/list_jira`;
export const JIRA_SALESFORCE_AGGS_LIST_SALESFORCE = `${VERSION}/jira_salesforce/list_salesforce`;
export const JIRA_SALESFORCE_AGGS_LIST_COMMIT = `${VERSION}/jira_salesforce/list_commit`;
export const JIRA_SALESFORCE_ESCALATION_TIME_REPORT = `${VERSION}/jira_salesforce/escalation_time_report`;
export const JIRA_SALESFORCE_FILES = `${VERSION}/jira_salesforce/files`;
export const JIRA_SALESFORCE_FILES_REPORT = `${VERSION}/jira_salesforce/files_report`;
export const JIRA_SALESFORCE_RESOLVED_TICKETS_TREND = `${VERSION}/jira_salesforce/resolved_tickets_trend`;

// triage rules
export const TRIAGE_RULES = `${VERSION}/triage_rules`;
export const TRIAGE_RULE_RESULT = `${VERSION}/triage_rule_results`;

// triage filters for grid view
export const TRIAGE_GRID_VIEW_FILTERS = `${VERSION}/triage_filters/grid-view`;
export const TRIAGE_FILTERS = `${VERSION}/triage_filters`;

// pager aggs
export const PAGERDUTY = `${VERSION}/pagerduty`;
export const PAGERDUTY_AGGRIGATIONS = `${PAGERDUTY}/aggregations`;
export const PAGERDUTY_RELEASE_INCIDENTS = `${PAGERDUTY_AGGRIGATIONS}/release_incidents`;
export const PAGERDUTY_ACK_TREND = `${PAGERDUTY_AGGRIGATIONS}/ack_trend`;
export const PAGERDUTY_AFTER_HOURS = `${PAGERDUTY_AGGRIGATIONS}/after_hours`;
export const PAGERDUTY_INCIDENT_RATES = `${PAGERDUTY_AGGRIGATIONS}/incidents_rate`;
export const PAGERDUTY_FILTER_VALUES = `${PAGERDUTY_AGGRIGATIONS}/values`;
export const PAGERDUTY_INCIDENTS = `${PAGERDUTY}/incidents`;
export const PAGERDUTY_INCIDENTS_AGGS = `${PAGERDUTY_AGGRIGATIONS}/incidents`;
export const PAGERDUTY_ALERTS_AGGS = `${PAGERDUTY_AGGRIGATIONS}/alerts`;
export const PAGERDUTY_RESOLUTION_TIME_REPORT = `${PAGERDUTY_AGGRIGATIONS}/resolution_time_report`;
export const PAGERDUTY_RESPONSE_TIME_REPORT = `${PAGERDUTY_AGGRIGATIONS}/response_time_report`;

//SonarQube
export const SONARQUBE = `${VERSION}/sonarqube_issues`;
export const SONARQUBE_FILTER_VALUES = `${SONARQUBE}/values`;
export const SONARQUBE_ISSUES_REPORT = `${SONARQUBE}/issue_report`;
export const SONARQUBE_EFFORT_REPORT = `${SONARQUBE}/effort_report`;

//SonarQube Metrics
export const SONARQUBE_METRICS = `${VERSION}/sonarqube_metrics`;
export const SONARQUBE_METRICS_FILTER_VALUES = `${SONARQUBE_METRICS}/values`;
export const SONARQUBE_METRICS_REPORT = `${SONARQUBE_METRICS}/report`;

// testrails
export const TESTRAILS_TESTS = `${VERSION}/testrails_tests`;
export const TESTRAILS_TESTS_LIST = `${TESTRAILS_TESTS}/list`;
export const TESTRAILS_TESTS_VALUES = `${TESTRAILS_TESTS}/values`;
export const TESTRAILS_TESTS_REPORT = `${TESTRAILS_TESTS}/tests_report`;
export const TESTRAILS_TESTS_ESTIMATE_REPORT = `${TESTRAILS_TESTS}/estimate_report`;
export const TESTRAILS_TESTS_ESTIMATE_FORECAST_REPORT = `${TESTRAILS_TESTS}/estimate_forecast_report`;
export const TESTRAILS_CUSTOM_FIELD = `${TESTRAILS_TESTS}/custom_case_fields`;
export const TESTRAILS_CUSTOM_FIELD_VALUES = `${TESTRAILS_CUSTOM_FIELD}/values`;

//levelops reports aggs
export const QUIZ_AGGS = `${QUIZ}/aggregations`;
export const QUIZ_AGGS_PAGINATED = `${QUIZ}/aggregations_paginated`;
export const WORK_ITEM_AGGS = `${WORK_ITEM}/aggregations`;

// praetorian
export const PRAETORIAN_ISSUES = `${VERSION}/praetorian/issues`;
export const PRAETORIAN_ISSUES_LIST = `${PRAETORIAN_ISSUES}/list`;
export const PRAETORIAN_ISSUES_VALUES = `${PRAETORIAN_ISSUES}/values`;
export const PRAETORIAN_ISSUES_AGGS = `${PRAETORIAN_ISSUES}/aggs`;

//bullseye
export const BULLSEYE_BUILDS = `${VERSION}/bullseye_builds`;
export const BULLSEYE_BUILDS_FILES = `${BULLSEYE_BUILDS}/files`;
export const BULLSEYE_BUILDS_COVERAGE_REPORT = `${BULLSEYE_BUILDS}/coverage_report`;
export const BULLSEYE_BUILDS_VALUES = `${BULLSEYE_BUILDS}/values`;

// ncc group
export const NCC_GROUP_ISSUES = `${VERSION}/ncc/issues`;
export const NCC_GROUP_ISSUES_LIST = `${NCC_GROUP_ISSUES}/list`;
export const NCC_GROUP_ISSUES_VALUES = `${NCC_GROUP_ISSUES}/values`;
export const NCC_GROUP_ISSUES_AGGS = `${NCC_GROUP_ISSUES}/aggs`;

// SNYK
export const SNYK_ISSUES = `${VERSION}/snyk_issues`;
export const SNYK_ISSUES_LIST = `${SNYK_ISSUES}/list`;
export const SNYK_ISSUES_VALUES = `${SNYK_ISSUES}/values`;
export const SNYK_ISSUES_REPORT = `${SNYK_ISSUES}/issues_report`;

// Azure
export const AZURE_PIPELINE = `${VERSION}/azure_devops_pipeline_runs`;
export const AZURE_JOB_DURATION = `${AZURE_PIPELINE}/duration_report`;
export const AZURE_RUNS_REPORT = `${AZURE_PIPELINE}/pipeline_runs_report`;

// JiraV2
export const JIRA_V2_GENERATE_TOKEN = `${VERSION}/atlassian-connect/otp/generate`;
export const JIRA_V2_VERIFY_CONNECTION = `${VERSION}/atlassian-connect/otp/claim`;

//Ticket Categorization Schemes
export const TICKET_CATEGORIZATION_SCHEMES = `${VERSION}/ticket_categorization_schemes`;

//Jenkins Integrations
export const JENKINS_INSTANCES = `${CICD}/instances`;
export const JENKINS_INSTANCES_ASSIGN = `${JENKINS_INSTANCES}/assign`;

// Velocity Configs
export const VELOCITY_CONFIGS = `${VERSION}/velocity_configs`;

// SCM PR Labels
export const SCM_PR_LABELS = `${VERSION}/scm/prs/labels`;

// SCM Review Collaboration
export const SCM_REVIEW_COLLABORATION = `${VERSION}/scm/prs/collab_report`;

// Lead Time Report
export const LEAD_TIME_REPORT = `${VERSION}/velocity`;
export const LEAD_TIME_VALUES = `${LEAD_TIME_REPORT}/values`;

// Reports Docs
export const DOCS = `${VERSION}/docs`;
export const REPORT_DOCS = `${DOCS}/reports`;
export const REPORT_DOCS_LIST = `${REPORT_DOCS}/list`;

// Organization
export const ORGANISATION = `${VERSION}/org`;
export const ORG_USERS = `${ORGANISATION}/users`;
export const CONTRIBUTORS_ROLES = `${ORG_USERS}/contributor_roles`;
//ORG Users version this may be changed
export const ORG_USERS_VERSIONS = `${ORG_USERS}/versions`;
export const ORG_USERS_IMPORT = `${ORG_USERS}/import`;
export const ORG_USERS_SCHEMA = `${ORG_USERS}/schema`;
export const ORG_USERS_FILTER = `${ORG_USERS}/values`;

// Ingestion
export const INGESTION = `${VERSION}/ingestion`;

//MFA
export const MFA = `${VERSION}/mfa`;
export const MFA_ENROLL = `${MFA}/enroll`;

//Azure Lead time filter values
export const ISSUE_MANAGEMENT = `${VERSION}/issue_mgmt/workitems`;
export const ISSUE_MANAGEMENT_WORKITEM = `${ISSUE_MANAGEMENT}/values`;
export const ISSUE_MANAGEMENT_TICKET_REPORT = `${ISSUE_MANAGEMENT}/tickets_report`;
export const ISSUE_MANAGEMENT_STAGE_TIME_REPORT = `${ISSUE_MANAGEMENT}/stage_times_report`;
export const ISSUE_MANAGEMENT_AGE_REPORT = `${ISSUE_MANAGEMENT}/age_report`;
export const ISSUE_MANAGEMENT_HYGIENE_REPORT = `${ISSUE_MANAGEMENT}/hygiene_report`;
export const ISSUE_MANAGEMENT_STORY_POINT_REPORT = `${ISSUE_MANAGEMENT}/story_point_report`;
export const ISSUE_MANAGEMENT_EFFORT_REPORT = `${ISSUE_MANAGEMENT}/effort_report`;
export const ISSUE_MANAGEMENT_SPRINT_REPORT = `${ISSUE_MANAGEMENT}/sprint_metrics_report`;
export const ISSUE_MANAGEMENT_CUSTOM_FIELD_VALUES = `${ISSUE_MANAGEMENT}/custom_field/values`;
export const ISSUE_MANAGEMENT_RESPONSE_TIME_REPORT = `${ISSUE_MANAGEMENT}/response_time_report`;
export const ISSUE_MANAGEMENT_RESOLUTION_TIME_REPORT = `${ISSUE_MANAGEMENT}/resolution_time_report`;
export const ISSUE_MANAGEMENT_HOPS_REPORT = `${ISSUE_MANAGEMENT}/hops_report`;
export const ISSUE_MANAGEMENT_BOUNCE_REPORT = `${ISSUE_MANAGEMENT}/bounce_report`;
export const ISSUE_MANAGEMENT_FIRST_ASSIGNEE_REPORT = `${ISSUE_MANAGEMENT}/first_assignee_report`;
export const ISSUE_MANAGEMENT_STAGE_BOUNCE_REPORT = `${ISSUE_MANAGEMENT}/stage_bounce_report`;

//WorkItem Fields List
export const ISSUE_MANAGEMENT_WORKITEM_FIELDS = `${VERSION}/issue_mgmt/workitems_fields`;

export const ISSUE_MANAGEMENT_SPRINTS = `${VERSION}/issue_mgmt/sprints`;
export const ISSUE_MANAGEMENT_WORKITEM_ATTRIBUTES_VALUES = `${ISSUE_MANAGEMENT}/attribute/values`;

//Workitem Priorities
export const WORKITEM_PRIORITIES = `${VERSION}/issue_mgmt/priorities`;

// Coverity
export const COVERITY = `${VERSION}/coverity`;
export const COVERITY_DEFECTS = `${COVERITY}/defects`;
export const DEFECTS_LIST = `${COVERITY_DEFECTS}/list`;
export const DEFECTS_VALUES = `${COVERITY_DEFECTS}/values`;
export const DEFECTS_REPORT = `${COVERITY_DEFECTS}/report`;

//code volume vs deployment volume
export const CODE_VOL_VS_DEPLOYMENT = `${COMBINED}/deploy_job_change_volume`;

// collections
export const ORG_UNITS = `${VERSION}/org/units`;
export const ORG_UNITS_VALUES = `${ORG_UNITS}/values`;
export const ORG_UNITS_INTEGRATION_ID = `${ORG_UNITS}/integrationid`;
export const ORG_PIVOT_LIST = `${VERSION}/org/groups/list`;
export const ORG_PIVOT_CREATE = `${VERSION}/org/groups`;
export const OU = `${VERSION}/ous`;
export const ORG_UNITS_DASHBOARDS = `${VERSION}/ous`;
// collections Versions
export const ORG_UNITS_VERSIONS = `${ORG_UNITS}/versions`;

export const DEV_PRODUCTIVITY_PROFILES = `${VERSION}/dev_productivity_profiles`;

//tenant state
export const TENANT_STATE = `${VERSION}/admin/tenant/state`;

// dev productivity
const DEV_PRODUCTIVITY = `${VERSION}/dev_productivity`;
export const DEV_PRODUCTIVITY_REPORTS = `${DEV_PRODUCTIVITY}/reports`;
export const DEV_PRODUCTIVITY_REPORTS_USERS = `${DEV_PRODUCTIVITY_REPORTS}/users`;
export const DEV_PRODUCTIVITY_USER_SNAPSHOT = `${VERSION}/snapshots/users`;
export const DEV_PRODUCTIVITY_REPORTS_ORGS = `${DEV_PRODUCTIVITY_REPORTS}/orgs`;
export const DEV_PRODUCTIVITY_FEATURE_DRILLDOWN = `${DEV_PRODUCTIVITY_REPORTS}/feature_details`;
export const DEV_PRODUCTIVITY_FIXED_INTERVALS = `${DEV_PRODUCTIVITY_REPORTS}/fixed_intervals`;
export const DEV_PRODUCTIVITY_FIXED_INTERVALS_ORGS = `${DEV_PRODUCTIVITY_FIXED_INTERVALS}/orgs`;
export const DEV_PRODUCTIVITY_FIXED_INTERVALS_USERS = `${DEV_PRODUCTIVITY_FIXED_INTERVALS}/users`;
export const DEV_PRODUCTIVITY_RELATIVE_SCORE = `${DEV_PRODUCTIVITY_REPORTS}/relative_score`;
export const DEV_PRODUCTIVITY_USERS_PR_ACTIVITY = `${DEV_PRODUCTIVITY_REPORTS_USERS}/scm_activity`;
export const DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT = `${VERSION}/scm/activity/list`;
export const DEVELOPER_RAW_STATS = `${VERSION}/dev_productivity/reports/fixed_intervals/raw_stats/org/users`;
export const DEVELOPER_RAW_STATS_NEW = `${VERSION}/dev_productivity/reports/fixed_intervals/users/list`;
export const ORG_RAW_STATS = `${VERSION}/dev_productivity/reports/fixed_intervals/raw_stats/orgs`;
export const ORG_RAW_STATS_NEW = `${VERSION}/dev_productivity/reports/fixed_intervals/orgs/list`;

// Effort Investment Jira
export const BA = `${VERSION}/ba`;
export const BA_JIRA = `${BA}/jira`;
export const BA_JIRA_ACTIVE = `${BA_JIRA}/active_work`;
export const BA_JIRA_TICKET_COUNT = `${BA_JIRA}/ticket_count_fte`;
export const BA_JIRA_STORY_POINT = `${BA_JIRA}/story_points_fte`;
export const BA_JIRA_ACTIVE_TICKET_COUNT = `${BA_JIRA_ACTIVE}/ticket_count`;
export const BA_JIRA_ACTIVE_STORY_POINT = `${BA_JIRA_ACTIVE}/story_points`;
export const BA_JIRA_TICKET_TIME_SPENT = `${BA_JIRA}/ticket_time_fte`;

//  Effort Investment Azure
export const BA_ISSUE_MANAGEMENT = `${BA}/issue_mgmt`;
export const BA_AZURE_TICKET_COUNT = `${BA_ISSUE_MANAGEMENT}/ticket_count_fte`;
export const BA_AZURE_STORY_POINT = `${BA_ISSUE_MANAGEMENT}/story_points_fte`;
export const BA_AZURE_COMMIT_COUNT = `${BA_ISSUE_MANAGEMENT}/commit_count_fte`;
export const BA_AZURE_TICKET_TIME_SPENT = `${BA_ISSUE_MANAGEMENT}/ticket_time_fte`;
export const BA_AZURE_ACTIVE = `${BA_ISSUE_MANAGEMENT}/active_work`;
export const BA_AZURE_ACTIVE_TICKET_COUNT = `${BA_AZURE_ACTIVE}/ticket_count`;
export const BA_AZURE_ACTIVE_STORY_POINT = `${BA_AZURE_ACTIVE}/story_points`;
export const BA_AZURE_TEAM_ALLOCATION = `${ISSUE_MANAGEMENT}/assignee_allocation_report`;

// issue filters
export const ISSUE_FILTERS = `${VERSION}/issue_filters`;
export const BA_SCM_JIRA_COMMIT_COUNT = `${BA_JIRA}/commit_count_fte`;

// Dora Reports

export const DORA_SCM = `${VERSION}/scm/dora`;
export const SCM_DORA_TIME_TO_RECOVER = `${DORA_SCM}/mean_time_to_recover`;
export const DORA_SCM_FAILURE_RATE = `${DORA_SCM}/failure_rate`;
// self onboarding repos uri
export const SELF_ONBOARDING_REPOS = `${VERSION}/scm_repos/repos/list`;
export const SELF_ONBOARDING_REPOS_SEARCH = `${VERSION}/scm_repos/search/repo`;

// workspace
export const WORKSPACE = `${ORGANISATION}/workspaces`;

// dora reports
export const DORA_URI = `${VERSION}/dora`;
export const CHANGE_FAILURE = `${VERSION}/dora/change_failure_rate`;
export const DEPLOYMENT_FREQUENCY = `${VERSION}/dora/deployment_frequency`;
export const DORA_DRILLDOWN_LIST = `${VERSION}/dora/drilldown/list`;
export const DORA_LEAD_TIME_FOR_CHANGE = `${DORA_URI}/lead_time`;
export const DORA_LEAD_TIME_FOR_CHANGE_DRILL_DOWN = `${DORA_LEAD_TIME_FOR_CHANGE}/drilldown/list`;
export const DORA_COMMITS_DRILL_DOWN = `${DORA_URI}/drilldown/scm-commits`;
export const CICD_JOB_PARAMS = `${DORA_URI}/cicd-job-params`;
export const LEAD_TIME_FOR_CHANGE = `${DORA_URI}/lead-time`;
export const MEAN_TIME_FOR_CHANGE = `${DORA_URI}/mean-time`;
export const LEAD_TIME_FOR_CHANGE_DRILLDOWN = `${LEAD_TIME_FOR_CHANGE}/drilldown`;
export const MEAN_TIME_FOR_CHANGE_DRILLDOWN = `${MEAN_TIME_FOR_CHANGE}/drilldown`;

// new trellis endpoints
export const DEV_PRODUCTIVITY_PARENT_PROFILE = `${VERSION}/dev_productivity_parent_profiles`;
export const DEV_PRODUCTIVITY_PARENT_PROFILE_DEFAULT = `${VERSION}/dev_productivity_parent_profiles/default`;
export const DEV_PRODUCTIVITY_PARENT_PROFILE_COPY = `${DEV_PRODUCTIVITY_PARENT_PROFILE}/copy`;
export const DEV_PRODUCTIVITY_PARENT_PROFILE_LIST = `${DEV_PRODUCTIVITY_PARENT_PROFILE}/list`;
