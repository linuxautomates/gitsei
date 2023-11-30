import * as dispatchActions from "reduxConfigs/maps/restapi";
import * as actionTypes from "reduxConfigs/actions/restapi";

import { getKBSelector, getTagsSelector } from "../selectors/restapiSelector";
import { cachedGenericRestApiRead } from "reduxConfigs/actions/cachedState.action";
import { getGenericWorkSpaceUUIDSelector } from "reduxConfigs/selectors/workspace/workspace.selector";

export const mapRestapiStatetoProps = state => {
  return {
    rest_api: state.restapiReducer,
    pagination_total: state.paginationReducer.pagination_total
  };
};

export const mapKBStateToProps = state => {
  return {
    bestpractices: getKBSelector(state)
  };
};

export const mapWorkspaceStateToProps = state => {
  return {
    workspace: workspaceId => getGenericWorkSpaceUUIDSelector(state, ...{ method: "get", id: workspaceId })
  };
};

export const mapTagsStateToProps = state => {
  return {
    tags: getTagsSelector(state)
  };
};

export const mapRestapiDispatchtoProps = dispatch => {
  return {
    teamGet: id => dispatch(actionTypes.teamGet(id)),
    teamsList: filters => dispatch(actionTypes.teamsList(filters)),
    teamsCreate: team => dispatch(actionTypes.teamsCreate(team)),
    orgsCreate: org => dispatch(actionTypes.orgsCreate(org)),
    orgsDelete: id => dispatch(actionTypes.orgsDelete(id)),
    orgsList: filters => dispatch(actionTypes.orgsList(filters)),
    orgsGet: id => dispatch(actionTypes.orgsGet(id)),
    orgsUpdate: (id, data) => dispatch(actionTypes.orgsUpdate(id, data)),
    releasesList: filters => dispatch(actionTypes.releasesList(filters)),
    releasesGet: id => dispatch(actionTypes.releasesGet(id)),
    filechangesList: filters => dispatch(actionTypes.filechangesList(filters)),
    checklistsGet: id => dispatch(actionTypes.checklistsGet(id)),
    gitreposList: (filters, complete = null) => dispatch(actionTypes.gitreposList(filters, complete)),
    jiraprojectsList: (filters, complete = null) => dispatch(actionTypes.jiraprojectsList(filters, complete)),
    fieldsList: (filters, complete = null) => dispatch(actionTypes.fieldsList(filters, complete)),
    repositoriesList: filters => dispatch(actionTypes.repositoriesList(filters)),
    repositoriesGet: id => dispatch(actionTypes.repositoriesGet(id)),
    eventLogsList: (filters, complete = null) => dispatch(actionTypes.eventLogsList(filters, complete)),
    metricsList: filters => dispatch(actionTypes.metricsList(filters)),
    ticketCategorizationSchemesList: (filters, id = 0) =>
      dispatch(actionTypes.ticketCategorizationSchemesList(filters, id)),
    cachedRestApiRead: (uri, method, id = "0", data, uniqueByKey = "", forceLoad = true) =>
      dispatch(cachedGenericRestApiRead(uri, method, id, data, "append", forceLoad, uniqueByKey)),
    ...dispatchActions.mapActivitylogsToProps(dispatch),
    ...dispatchActions.mapBestpracticesToProps(dispatch),
    ...dispatchActions.mapFilesToProps(dispatch),
    ...dispatchActions.mapIntegrationsToProps(dispatch),
    ...dispatchActions.mapMappingsToProps(dispatch),
    ...dispatchActions.mapNotesToProps(dispatch),
    ...dispatchActions.mapProductsToProps(dispatch),
    ...dispatchActions.mapQuestionnairesToProps(dispatch),
    ...dispatchActions.mapSectionsToProps(dispatch),
    ...dispatchActions.mapQuizToProps(dispatch),
    ...dispatchActions.mapRestapiToProps(dispatch),
    ...dispatchActions.mapSignaturesToProps(dispatch),
    ...dispatchActions.mapSSOToProps(dispatch),
    ...dispatchActions.mapStatesToProps(dispatch),
    ...dispatchActions.mapStagesToProps(dispatch),
    ...dispatchActions.mapTagsToProps(dispatch),
    ...dispatchActions.mapToolsToProps(dispatch),
    ...dispatchActions.mapUsersToProps(dispatch),
    ...dispatchActions.mapWorkflowsToProps(dispatch),
    ...dispatchActions.mapWorkitemsToProps(dispatch),
    ...dispatchActions.mapcTemplatesToProps(dispatch),
    ...dispatchActions.mapApikeysToProps(dispatch),
    ...dispatchActions.mapPluginsToProps(dispatch),
    ...dispatchActions.mapPluginresultsToProps(dispatch),
    ...dispatchActions.mapSmartTicketTemplatesToProps(dispatch),
    ...dispatchActions.mapCustomFieldsDispatchToProps(dispatch),
    ...dispatchActions.mapReportsToProps(dispatch),
    ...dispatchActions.mapPluginLabelsToProps(dispatch),
    ...dispatchActions.mapProductaggsToProps(dispatch),
    ...dispatchActions.mapPropelNodeTemplatesToProps(dispatch),
    ...dispatchActions.mapPropelTriggerTemplatesToProps(dispatch),
    ...dispatchActions.mapPrepelsToProps(dispatch),
    ...dispatchActions.mapPropelTriggerTemplatesToProps(dispatch),
    ...dispatchActions.mapPropelTriggerEventsToProps(dispatch),
    ...dispatchActions.mapPropelRunsToProps(dispatch),
    ...dispatchActions.mapPropelReportsToProps(dispatch),
    ...dispatchActions.mapPropelNodeCategoriesToProps(dispatch),
    ...dispatchActions.mapConfigsToProps(dispatch),
    ...dispatchActions.mapContentSchemaToProps(dispatch),
    ...dispatchActions.mapWidgetFilterValueToProps(dispatch),
    ...dispatchActions.mapDashboardToProps(dispatch),
    ...dispatchActions.mapJiraPrioritiesToProps(dispatch),
    ...dispatchActions.mapJiraIntegConfigsToProps(dispatch),
    ...dispatchActions.mapJiraFieldsToProps(dispatch),
    ...dispatchActions.mapJiraZendeskProps(dispatch),
    ...dispatchActions.maptriageRulesToProps(dispatch),
    ...dispatchActions.mapConfigTablesToProps(dispatch),
    ...dispatchActions.maptriageToProps(dispatch),
    ...dispatchActions.mapDashboardReportsToProps(dispatch),
    ...dispatchActions.mapDashboardReportsToProps(dispatch),
    ...dispatchActions.mapJiraSalesforceProps(dispatch),
    ...dispatchActions.mapAzurePrioritiesToProps(dispatch),
    ...dispatchActions.mapWorkspaceToProps(dispatch)
  };
};
