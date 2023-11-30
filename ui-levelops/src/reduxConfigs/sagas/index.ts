import { all } from "redux-saga/effects";
import {
  changePasswordWatcherSaga,
  passwordResetWatcherSaga,
  sessionWatcherSaga,
  ssoWatcherSaga,
  sessionLogoutWatcherSaga,
  sessionMFAWatcherSaga,
  sessionGetMeWatcherSaga
} from "./authentication.saga";
import { paginationWatcherSaga } from "./paginationSaga";
import { watchRequests, watchWriteRequests } from "./restapiSaga";
import { workbenchTabCountWatcherSaga } from "./tabCountSaga";
import { workItemFlowWatcherSaga } from "./workitemFlowSaga";
import { propelFetchWatcherSaga, propelNewWatcherSaga } from "./propelFetch.saga";
import { exportAssessmentWatcherSaga } from "./exportAssessmentSaga";
import { importAssessmentWatcherSaga } from "./importAssessmentSaga";
import { getOrCreateTagsWatcherSaga } from "./getOrCreateTagsSaga";
import {
  azureHygieneReportSagaWatcher,
  azureHygieneReportTrendSagaWatcher,
  hygieneFetchWatcherSaga,
  hygieneTrendFetchWatcherSaga
} from "./hygiene.saga";
import {
  jiraFiltersPreFetchedWatcherSaga,
  jiraFiltersWatcherSaga,
  jiraSalesforceFiltersWatcherSaga,
  jiraZendeskFiltersWatcherSaga,
  leadTimeFiltersEffectWatcherSaga
} from "./jiraFilterValues.saga";
import { genericFiltersWatcherSaga } from "./genericFilterValues.saga";
import {
  csvDownloadDrilldownWatcherSaga,
  csvDownloadTriageGridViewWatcherSaga,
  usersCSVDownloadWatcherSaga,
  usersSampleCSVDownloadWatcherSaga
} from "./csvDownload.saga";
import { jiraZendeskWatcherSaga } from "./jiraZendesk.saga";
import { jiraSalesforceWatcherSaga } from "./jiraSalesforce.saga";
import { getOrCreateUsersWatcherSaga } from "./getOrCreateUsers.saga";
import { getOrCreateKBWatcherSaga } from "./getOrCreateKnowledgeBaseSaga";
import { triageMatchingJobsWatcherSaga } from "./triageMatchingJobs";
import { fetchJobResultsWatcherSaga } from "./fetchJobResultsSaga";
import { levelOpsWidgetsWatcherSaga } from "./levelopsWidgets.saga";
import { genericIdsMapWatcherSaga } from "./genericIdsMap.saga";
import { jiraZendeskSalesforceStagesWidgetsWatcher } from "./jiraSalesforceZendeskWidgets.saga";
import { jiraZendeskSalesforceC2FWidgetsWatcher } from "./jiraSalesforceZendeskC2FWidgets.saga";
import { assessmentTemplateSaveWatcherSaga, assessmentTemplateGetWatcherSaga } from "./assessmentTemplateSaga";
import { genericTableCSVDownloadWatcherSaga } from "./csv-download-saga";
import { supportedFilterGetWatcher } from "./supportedFilterGet.saga";
import { defaultDashboardWatcherSaga } from "./defaultDashboardSaga";
import { teanantStateWatcherSaga } from "./tenantStateSaga";
import { deleteWidgetSagaWatcher } from "./widgets/deleteWidgetSaga";
import { changeWidgetsOrderSagaWatcher } from "./widgets/changeWidgetsOrderSaga";
import { zendeskFiltersWatcherSaga } from "./zendeskFilterValues.saga";
import { dashboardUpdateSagaWatcher } from "./dashboards/dashboardUpdateSaga";
import { bulkUpdateWidgetSagaWatcher } from "./widgets/bulkUpdateWidgetSaga";
import {
  multiWidgetReportAddSagaWatcher,
  multiWidgetReportDeleteSagaWatcher,
  multiWidgetReportNameUpdateSagaWatcher
} from "./widgets/multiReportSelectSaga";
import { loadDashboardSagaWatcher } from "./dashboards/loadDashboardSaga";
import { jiraBAProgressStatWatcherSaga } from "./jiraBAProgressStat.saga";
import { jiraBAProgressReportWatcherSaga } from "./jiraBAProgressReportSaga";
import { deleteTicketCategorizationSchemeSagaWatcher } from "./ticket-categorization/deleteTicketCategorizationScheme.saga";
import { listCloneTicketCategorizationSchemeSagaWatcher } from "./ticket-categorization/cloneTicketCategorizationScheme.saga";
import { setDefaultTicketCategorizationSchemeSagaWatcher } from "./ticket-categorization/setDefaultTicketCategorizationScheme.saga";
import { resetTicketCategorizationSchemeColorPaletteSagaWatcher } from "./ticket-categorization/resetTicketCategorizationSchemeColorPalette.saga";
import { jiraEpicPriorityReportWatcherSaga } from "./jiraEpicPrioritySaga";
import { jiraBurnDownReportWatcherSaga } from "./jiraBurnDownReportSaga";
import { jiraEffortInvestmentStatWatcherSaga } from "./jiraEffortInvestMentStatSaga";
import { jiraBAEffortInvestmentTrendReportWatcher } from "./jiraBAEffortInvestmentTrendReportSaga";
import { jenkinsIntegrationWatcherSaga } from "./jenkinsSaga";
import { effortInvestmentTeamReportWatcher } from "./effortInvestmentTeamReportSaga";
import { jiraSprintReportsList, jiraSprintFilterList } from "./jirasprintReports";
import { cloneVelocityConfigSagaWatcher } from "./velocity-configs/cloneVelocityConfig.saga";
import { setToDefaultVelocityConfigSagaWatcher } from "./velocity-configs/setToDefaultVelocityConfig.saga";
import { deleteVelocityConfigSagaWatcher } from "./velocity-configs/deleteVelocityConfig.saga";
import {
  azureCategoriesFiltersValueSagaWatcher,
  categoriesFiltersValueSagaWatcher
} from "./ticket-categorization/categoriesFiltersValues.saga";
import {
  azureCategoriesTrellisProfileFiltersValueSagaWatcher,
  categoriesFiltersTrellisProfileValueSagaWatcher
} from "./ticket-categorization/categoriesFiltersValuesTrellisProfiles.saga";
import { assigneeTimeReportWatcherSaga } from "./assigneeTimeReportSaga";
import { reportListSagaWatcher } from "./widgets/listReportSaga";
import { loadDashboardIntegrationWatcher } from "./integrations/loadDashboardIntegrationsSaga";
import { integrationIngestionWatcherSaga } from "./integrationIngestion.Saga";
import { mfaEnrollGetWatcherSaga, mfaEnrollPostWatcherSaga } from "./mfa_enroll.saga";
import {
  updateWidgetSagaWatcher,
  widgetDrilldownColumnUpdateSagaWatcher,
  widgetMetaDataUpdateSagaWatcher,
  widgetSelectedColumnUpdateSagaWatcher,
  widgetTableFiltersUpdateSagaWatcher
} from "./widgets/widgetUpdateSaga";
import { copyWidgetSagaWatcher } from "./widgets/copyWidgetSaga";
import { azureFilterValueWatcherSaga } from "./azureFilterValueSaga";
import { issueFiltersEffectWatcherSaga } from "./issueFilterEffectSaga";
import { slaModuleWatcher } from "./slaModuleSaga";
import { genericStoredIdsMapWatcherSaga } from "./genericStoredIdsMap.saga";
import { getVelocityConfigSagaWatcher } from "./velocity-configs/getVelocityConfig.saga";
import {
  azureCustomFieldListWatcherSaga,
  jiraCustomFilterFieldListWatcherSaga,
  zendeskCustomFieldListWatcherSaga,
  testrailsCustomFieldListWatcherSaga
} from "./customFilterFieldList.saga";
import { CodeVolVsDeployemntValueWatcherSaga } from "./codeVolVsDploymentValuesSaga";
import { deleteOrgUnitSagaWatcher } from "./organization-units/deleteOrgUnit.saga";
import { cloneOrgUnitSagaWatcher } from "./organization-units/cloneOrgUnit.saga";
import { createOrgUnitSagaWatcher } from "./organization-units/orgUnitCreate.saga";
import { orgUnitFiltersValueSagaWatcher } from "./organization-units/orgUnitFilterValues.saga";
import { orgUnitUtitlitySagaWatcher } from "./organization-units/orgUnitUtilitySaga";
import { updateOrgUnitSagaWatcher } from "./organization-units/orgUnitUpdate.saga";
import { orgUnitGetSagaWatcher } from "./organization-units/orgUnitGetSaga";
import { dashboardTimeUpdateWidgetsWatcher } from "./dashboardTimeUpdateSaga";
import { restApiSelectGenericListWatcher } from "./restApiSelectGenericList.Saga";
import {
  jiraEIActiveEngineerReportWatcherSaga,
  jiraEICompletedEngineerReportWatcherSaga
} from "./jiraEIEngineerReportSaga";
import { jiraEffortAlignmentWatcher } from "./jiraEffortAlignmentReportSaga";
import { genericReportCSVTransformWatcher } from "./csv-download-saga/genericReportCSVTransform.saga";
import { ouScoreOverviewWatcherSaga } from "./trellisProfileSaga/ouOverviewSaga";
import { apiFilterWatcherSaga } from "./apiFilter.saga";
import { loadDashboardIntegrationConfigWatcher } from "./integrations/loadDashboardIntegrationConfig";
import { devProductivityCSVDownloadWatcherSaga } from "./trellisProfileSaga/devProductivityCsvSaga";
import { devProductivityScoreReportWatcherSaga } from "./devProductivity.saga";
import { devProductivityPRActivityWatcherSaga } from "./trellisProfileSaga/prActivitySaga";
import { loadDashboardFieldsListWatcher } from "./integrations/loadDashboardFieldsList";
import { filterWidgetsCustomFieldsSagaWatcher } from "./widgets/filterWidgetsCustomFieldsSaga";
import { orgCustomDataWatcherSaga } from "./organization-units/orgCustomData.saga";
import { cachedRestApiSagaWatcher } from "./cachedRestApiSaga";
import { satelliteIntegrationYAMLDownloadWatcherSaga } from "./self-onboarding/satelliteIntegrationYAMLDownloadSaga";
import { integrationMonitoringWatcherSaga } from "./integrationMonitoringSaga";
import { getDataForWidgetsUpdationWatcherSaga } from "./dashboards/updateDashboardWidgetsSaga";
import { leadTimeByTimeSpentInStagesWatcherSaga } from "./leadTimeByTimeSpentInStages.saga";
import { trellisProfileCloneWatcherSaga } from "./trellisProfileSaga/cloneTrellisProfileSaga";
import { deleteTrellisProfileSagaWatcherSaga } from "./trellisProfileSaga/deleteTrellisProfileSaga";
import { trellisProfileListWatcherSaga } from "./trellisProfileSaga/trellisProfilesListSaga";
import { trellisProfileDetailsWatcherSaga } from "./trellisProfileSaga/trellisProfileDetailsSaga";
import { trellisProfileUpdateWatcherSaga } from "./trellisProfileSaga/trellisProfileUpdateSaga";
import { trellisProfileCreateWatcherSaga } from "./trellisProfileSaga/trellisProfileCreateSaga";
import { OrgUnitDashboardsSagaWatcher } from "./organization-units/orgUnitDashboards.saga";
import { SelectedWorkspaceSagaWatcher, WorkspaceSagaWatcher } from "./workspace.saga";
import { devRawStatsWatcherSaga } from "./widgetAPI/devRawStatsSaga";
import { orgRawStatsWatcherSaga } from "./widgetAPI/orgRawStatsSaga";
import { rawStatsCSVDownloadWatcherSaga } from "./trellisProfileSaga/rawStatsCsvSaga";
import { devRawGraphStatsWatcherSaga } from "./widgetGraphAPI/devRawStatsGraphSaga";
import { ouDashboardSetDefaultSagaWatcher } from "./organization-units/ouDashboardSetDefaultSaga";
import { workspaceOUListWatcherSaga } from "./trellisProfileSaga/workspaceOrgSaga";
import { workspaceCategoriesSagaWatcher } from "./workspaceCategoriesSaga";
import { getDashboardOUSagaWatcher } from "./dashboards/getDashboardOUOptions.saga";
import { getOUFiltersSagaWatcher } from "./dashboards/getOUFiltersSaga";
import { cachedIntegrationSagaWatcher } from "./integrations/cachedIntegrationSaga";
import { azureBAProgressReportWatcher } from "./azureBAProgressReportSaga";
import { effortInvestmentCreateUpdateSagaWatcher } from "./ticket-categorization/effortInvestmentCreateUpdateSaga";
import { updateDashboardOUSagaWatcher } from "./dashboards/updateSelectedDashboard.saga";
import { azureBAProgramProgressReportWatcher } from "./azureBAProgramProgressReportSaga";
import { workflowProfileCreateWatcherSaga } from "./workflowProfileSaga/workflowProfileCreateSaga";
import { workflowProfileUpdateWatcherSaga } from "./workflowProfileSaga/workflowProfileUpdateSaga";
import { workflowProfileDetailsWatcherSaga } from "./workflowProfileSaga/workflowProfileDetailSaga";
import { workflowProfileByOuCreateWatcherSaga } from "./workflowProfileSaga/workflowProfileByOuSaga";
import { doraChangeFailureWatcherSaga } from "./widgetAPI/doraChangeFailureSaga";
import { doraDepoymentFrequencyWatcherSaga } from "./widgetAPI/doraDeploymentFrequencySaga";
import { cloneWorkflowProfileSagaWatcher } from "./workflowProfileSaga/cloneWorkflowProfileSaga";
import { userTrellisPermissionUpdateSagaWatcher } from "./trellisUserPermissionSaga";
import { doraLeadTimeForChangeWatcherSaga } from "./widgetAPI/DoraLeadTimeForChangeSaga";
import { getWorkflowProfileFilterWatcherSaga } from "./workflowProfileSaga/getWorkflowProfileFiltersSaga";
import { tableReportRestApiSagaWatcher } from "./reports/table-report/tableReportSaga";
import { getCICDJobParamsSaga } from "./workflowProfileSaga/getCICDJobParamsSaga";
import { doraLeadTimeWidgetWatcherSaga, doraMeanTimeToRestoreWatcherSaga } from "./widgetAPI/DoraLeadTimeMTTRSaga";
import { trellisProfileOuAssociateSaga } from "./trellisProfileSaga/trellisProfileOuAssociateSaga";
import { workflowProfileOuAssociationSagaWatcher } from "./workflowProfileSaga/workflowProfileOuAssociationSaga";
import { trellisProfilePartialUpdateWatcherSaga } from "./trellisProfileSaga/trellisProfilePartialUpdateSaga";
import { testrailsFilterValueWatcherSaga } from "./testrailsFilterValueSaga";
import { jiraReleaseTableReportWatcherSaga } from "./reports/jira-release-table-report/jiraReleaseTableSaga";
import { trellisProfileOUListWatcherSaga } from "./trellis-ou-profile-saga/trellisOUProfileListSaga";
import { trellisProfileOUUpdateWatcherSaga } from "./trellis-ou-profile-saga/trellisOUProfileUpdateSaga";
import { trellisOUCentralProfileSagaWatcherSaga } from "./trellis-ou-profile-saga/trellisOUCentralProfileSaga";
import { trellisCentralProfileUpdateWatcherSaga } from "./trellis-ou-profile-saga/trellisCentralProfileUpdateSaga";

// import watchers from other files
export default function* rootSaga() {
  yield all([
    sessionWatcherSaga(),
    ssoWatcherSaga(),
    sessionLogoutWatcherSaga(),
    sessionMFAWatcherSaga(),
    // add other watchers to the array
    paginationWatcherSaga(),
    //restapiChangeWatcherSaga(),
    watchRequests(),
    watchWriteRequests(),
    passwordResetWatcherSaga(),
    workbenchTabCountWatcherSaga(),
    changePasswordWatcherSaga(),
    workItemFlowWatcherSaga(),
    propelFetchWatcherSaga(),
    propelNewWatcherSaga(),
    exportAssessmentWatcherSaga(),
    importAssessmentWatcherSaga(),
    getOrCreateTagsWatcherSaga(),
    getOrCreateUsersWatcherSaga(),
    hygieneFetchWatcherSaga(),
    azureHygieneReportSagaWatcher(),
    azureHygieneReportTrendSagaWatcher(),
    hygieneTrendFetchWatcherSaga(),
    jiraFiltersWatcherSaga(),
    jiraFiltersPreFetchedWatcherSaga(),
    genericFiltersWatcherSaga(),
    csvDownloadDrilldownWatcherSaga(),
    csvDownloadTriageGridViewWatcherSaga(),
    jiraZendeskWatcherSaga(),
    jiraZendeskFiltersWatcherSaga(),
    jiraSalesforceWatcherSaga(),
    jiraSalesforceFiltersWatcherSaga(),
    getOrCreateKBWatcherSaga(),
    triageMatchingJobsWatcherSaga(),
    fetchJobResultsWatcherSaga(),
    levelOpsWidgetsWatcherSaga(),
    genericIdsMapWatcherSaga(),
    assessmentTemplateSaveWatcherSaga(),
    assessmentTemplateGetWatcherSaga(),
    jiraZendeskSalesforceStagesWidgetsWatcher(),
    jiraZendeskSalesforceC2FWidgetsWatcher(),
    genericTableCSVDownloadWatcherSaga(),
    supportedFilterGetWatcher(),
    defaultDashboardWatcherSaga(),
    teanantStateWatcherSaga(),
    deleteWidgetSagaWatcher(),
    changeWidgetsOrderSagaWatcher(),
    zendeskFiltersWatcherSaga(),
    dashboardUpdateSagaWatcher(),
    bulkUpdateWidgetSagaWatcher(),
    multiWidgetReportAddSagaWatcher(),
    multiWidgetReportDeleteSagaWatcher(),
    multiWidgetReportNameUpdateSagaWatcher(),
    loadDashboardSagaWatcher(),
    jiraBAProgressStatWatcherSaga(),
    jiraBAProgressReportWatcherSaga(),
    jiraEpicPriorityReportWatcherSaga(),
    jiraBurnDownReportWatcherSaga(),
    jiraEffortInvestmentStatWatcherSaga(),
    jiraBAEffortInvestmentTrendReportWatcher(),
    effortInvestmentTeamReportWatcher(),
    deleteTicketCategorizationSchemeSagaWatcher(),
    listCloneTicketCategorizationSchemeSagaWatcher(),
    setDefaultTicketCategorizationSchemeSagaWatcher(),
    categoriesFiltersValueSagaWatcher(),
    categoriesFiltersTrellisProfileValueSagaWatcher(),
    jenkinsIntegrationWatcherSaga(),
    jiraSprintReportsList(),
    jiraSprintFilterList(),
    cloneVelocityConfigSagaWatcher(),
    setToDefaultVelocityConfigSagaWatcher(),
    deleteVelocityConfigSagaWatcher(),
    assigneeTimeReportWatcherSaga(),
    leadTimeFiltersEffectWatcherSaga(),
    reportListSagaWatcher(),
    loadDashboardIntegrationWatcher(),
    integrationIngestionWatcherSaga(),
    mfaEnrollGetWatcherSaga(),
    mfaEnrollPostWatcherSaga(),
    updateWidgetSagaWatcher(),
    copyWidgetSagaWatcher(),
    azureFilterValueWatcherSaga(),
    azureCategoriesFiltersValueSagaWatcher(),
    azureCategoriesTrellisProfileFiltersValueSagaWatcher(),
    issueFiltersEffectWatcherSaga(),
    slaModuleWatcher(),
    genericStoredIdsMapWatcherSaga(),
    getVelocityConfigSagaWatcher(),
    jiraCustomFilterFieldListWatcherSaga(),
    azureCustomFieldListWatcherSaga(),
    CodeVolVsDeployemntValueWatcherSaga(),
    usersCSVDownloadWatcherSaga(),
    usersSampleCSVDownloadWatcherSaga(),
    cloneOrgUnitSagaWatcher(),
    deleteOrgUnitSagaWatcher(),
    createOrgUnitSagaWatcher(),
    orgUnitFiltersValueSagaWatcher(),
    orgUnitUtitlitySagaWatcher(),
    updateOrgUnitSagaWatcher(),
    orgUnitGetSagaWatcher(),
    dashboardTimeUpdateWidgetsWatcher(),
    restApiSelectGenericListWatcher(),
    jiraEIActiveEngineerReportWatcherSaga(),
    jiraEICompletedEngineerReportWatcherSaga(),
    jiraEffortAlignmentWatcher(),
    genericReportCSVTransformWatcher(),
    zendeskCustomFieldListWatcherSaga(),
    ouScoreOverviewWatcherSaga(),
    devProductivityScoreReportWatcherSaga(),
    apiFilterWatcherSaga(),
    loadDashboardIntegrationConfigWatcher(),
    devProductivityCSVDownloadWatcherSaga(),
    devProductivityPRActivityWatcherSaga(),
    loadDashboardFieldsListWatcher(),
    filterWidgetsCustomFieldsSagaWatcher(),
    resetTicketCategorizationSchemeColorPaletteSagaWatcher(),
    orgCustomDataWatcherSaga(),
    sessionGetMeWatcherSaga(),
    cachedRestApiSagaWatcher(),
    satelliteIntegrationYAMLDownloadWatcherSaga(),
    integrationMonitoringWatcherSaga(),
    widgetDrilldownColumnUpdateSagaWatcher(),
    getDataForWidgetsUpdationWatcherSaga(),
    leadTimeByTimeSpentInStagesWatcherSaga(),
    trellisProfileCloneWatcherSaga(),
    deleteTrellisProfileSagaWatcherSaga(),
    trellisProfileListWatcherSaga(),
    trellisProfileDetailsWatcherSaga(),
    trellisProfileUpdateWatcherSaga(),
    trellisProfileCreateWatcherSaga(),
    OrgUnitDashboardsSagaWatcher(),
    WorkspaceSagaWatcher(),
    SelectedWorkspaceSagaWatcher(),
    devRawStatsWatcherSaga(),
    orgRawStatsWatcherSaga(),
    widgetSelectedColumnUpdateSagaWatcher(),
    rawStatsCSVDownloadWatcherSaga(),
    devRawGraphStatsWatcherSaga(),
    ouDashboardSetDefaultSagaWatcher(),
    widgetTableFiltersUpdateSagaWatcher(),
    workspaceOUListWatcherSaga(),
    workspaceCategoriesSagaWatcher(),
    getDashboardOUSagaWatcher(),
    getOUFiltersSagaWatcher(),
    cachedIntegrationSagaWatcher(),
    azureBAProgressReportWatcher(),
    effortInvestmentCreateUpdateSagaWatcher(),
    updateDashboardOUSagaWatcher(),
    azureBAProgramProgressReportWatcher(),
    workflowProfileCreateWatcherSaga(),
    workflowProfileUpdateWatcherSaga(),
    workflowProfileDetailsWatcherSaga(),
    workflowProfileByOuCreateWatcherSaga(),
    doraChangeFailureWatcherSaga(),
    doraDepoymentFrequencyWatcherSaga(),
    cloneWorkflowProfileSagaWatcher(),
    userTrellisPermissionUpdateSagaWatcher(),
    doraLeadTimeForChangeWatcherSaga(),
    getWorkflowProfileFilterWatcherSaga(),
    tableReportRestApiSagaWatcher(),
    getCICDJobParamsSaga(),
    doraLeadTimeWidgetWatcherSaga(),
    doraMeanTimeToRestoreWatcherSaga(),
    widgetMetaDataUpdateSagaWatcher(),
    trellisProfileOuAssociateSaga(),
    workflowProfileOuAssociationSagaWatcher(),
    trellisProfilePartialUpdateWatcherSaga(),
    testrailsCustomFieldListWatcherSaga(),
    testrailsFilterValueWatcherSaga(),
    jiraReleaseTableReportWatcherSaga(),
    trellisProfileOUListWatcherSaga(),
    trellisProfileOUUpdateWatcherSaga(),
    trellisOUCentralProfileSagaWatcherSaga(),
    trellisCentralProfileUpdateWatcherSaga()
  ]);
}
