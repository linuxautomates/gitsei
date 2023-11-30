import * as actions from "../actionTypes";
import { RestTeams } from "../../../classes/RestTeams";
import { RestOrganizations } from "../../../classes/RestOrganizations";

// team actions
export const teamsList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "teams",
  function: "getTeams",
  method: "list"
});

export const teamGet = teamId => ({
  type: actions.RESTAPI_READ,
  id: teamId,
  uri: "teams",
  function: "getTeam",
  method: "get",
  validator: RestTeams
});

export const teamsDelete = teamId => ({
  type: actions.RESTAPI_WRITE,
  id: teamId
});

export const teamsCreate = team => ({ type: actions.RESTAPI_WRITE, data: team });

export const teamsUpdate = (teamId, team) => ({ type: actions.RESTAPI_WRITE, id: teamId, data: team });

// team mappings actions

// org actions

export const orgsList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "orgs",
  function: "getOrganizations",
  method: "list"
});

export const orgsGet = organizationId => ({
  type: actions.RESTAPI_READ,
  id: organizationId,
  uri: "orgs",
  function: "getOrganization",
  method: "get",
  validator: RestOrganizations
});

export const orgsDelete = organizationId => ({
  type: actions.RESTAPI_WRITE,
  id: organizationId,
  uri: "orgs",
  function: "deleteOrganization",
  method: "delete"
});

export const orgsCreate = org => ({
  type: actions.RESTAPI_WRITE,
  data: org,
  uri: "orgs",
  function: "createOrganization",
  method: "create"
});

export const orgsUpdate = (organizationId, org) => ({ type: actions.RESTAPI_WRITE, data: org, id: organizationId });

// releases

export const releasesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "releases",
  function: "getReleases",
  method: "list"
});

export const releasesGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: "releases",
  function: "getRelease",
  method: "get"
});

// repos

export const reposList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "repos",
  function: "getRepos",
  method: "list"
});

export const reposSeries = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "repos",
  function: "getRepoSeries",
  method: "series"
});

// technologies

export const technologiesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "technologies",
  function: "getTechnologies",
  method: "list"
});

// deployments

export const deploymentsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "deployments",
  function: "getDeployments",
  method: "list"
});

// filechanges

export const filechangesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "filechanges",
  function: "getFilechanges",
  method: "list"
});

export const alertsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "alerts",
  function: "getAlerts",
  method: "list"
});

export const alertsSeries = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "alerts",
  function: "getAlertsSeries",
  method: "series"
});

export const alertsAggregate = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "alerts",
  function: "getAlertsAggregate",
  method: "aggregate"
});

export const resourcesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "resources",
  function: "getResources",
  method: "list"
});

export const accountsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "accounts",
  function: "getAccounts",
  method: "list"
});

export const developersList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "developers",
  function: "getDevelopers",
  method: "list"
});

export const checklistsGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: "checklists",
  function: "getChecklist",
  method: "get"
});

export const gitreposList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "gitrepos",
  function: "getGitRepos",
  method: "list"
});

export const jiraprojectsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "jiraprojects",
  function: "getJiraProjects",
  method: "list"
});

export const fieldsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "fields",
  function: "getFields",
  method: "list"
});

export const repositoriesList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "repositories",
  function: "getRepositories",
  method: "list"
});

export const repositoriesGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: "repositories",
  function: "getRepository",
  method: "get"
});

export const eventLogsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: "event_logs",
  function: "getEventLogs",
  method: "list"
});

export const metricsList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "metrics",
  function: "getMetrics",
  method: "list"
});
