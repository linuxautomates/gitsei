import { restInstance } from "utils/restRequest.js";
import {
  ACCOUNTS,
  ACTIVITYLOGS,
  ALERTS,
  BPS,
  CHECKLISTS,
  COMMS,
  CTEMPLATE,
  DASHBOARDS,
  DEPLOYMENTS,
  DEVELOPERS,
  EVENT_LOGS,
  FIELDS,
  FILE_UPLOAD,
  FILECHANGES,
  GITREPOS,
  INTEGRATION,
  INTEGRATIONS,
  JIRAPROJECTS,
  MAPPINGS,
  METRICS,
  NOTES,
  NOTIFICATIONS,
  ORGS,
  PRODUCTS,
  QUERY,
  QUESTIONNAIRES,
  QUIZ,
  RBAC,
  RBACS,
  RELEASES,
  REPOS,
  REPOSITORIES,
  RESOURCES,
  SAML_CONFIG,
  SECTIONS,
  SIGNATURE_LOGS,
  SIGNATURES,
  STAGES,
  TAGS,
  TEAMS,
  TECHNOLOGIES,
  TOOLS,
  USER_PROFILE,
  USERS,
  WORK_ITEM,
  WORKFLOWS
} from "constants/restUri.js";
import { unset } from "lodash";

export default class BackendService {
  /*
    All calls to rest api backend, except for the auth calls that are saved elsewhere
    If cancel token is not provided, then a token will not be added to the request
     */

  constructor() {
    this.restInstance = restInstance;
    this.options = {};
    this.createOrganization = this.createOrganization.bind(this);
    this.getOrganizations = this.getOrganizations.bind(this);
    this.deleteOrganization = this.deleteOrganization.bind(this);
    this.getUsers = this.getUsers.bind(this);
    this.getIntegrations = this.getIntegrations.bind(this);
    this.createIntegration = this.createIntegration.bind(this);
    this.getIntegration = this.getIntegration.bind(this);
    this.deleteIntegration = this.deleteIntegration.bind(this);
    this.updateIntegration = this.updateIntegration.bind(this);
    this.getDashboard = this.getDashboard.bind(this);
    this.getDashboards = this.getDashboards.bind(this);
    this.getQuery = this.getQuery.bind(this);
    this.getNotifications = this.getNotifications.bind(this);
    this.getRbacs = this.getRbacs.bind(this);
    this.addUser = this.addUser.bind(this);
    this.getUser = this.getUser.bind(this);
    this.updateUser = this.updateUser.bind(this);
    this.deleteUser = this.deleteUser.bind(this);
    this.updateRbac = this.updateRbac.bind(this);
    this.getOrganization = this.getOrganization.bind(this);
    this.getTeams = this.getTeams.bind(this);
    this.getTeam = this.getTeam.bind(this);
    this.createTeam = this.createTeam.bind(this);
    this.updateTeam = this.updateTeam.bind(this);
    this.deleteTeam = this.deleteTeam.bind(this);
    this.createMapping = this.createMapping.bind(this);
    this.updateMapping = this.updateMapping.bind(this);
    this.deleteMapping = this.deleteMapping.bind(this);
    this.getMappings = this.getMappings.bind(this);
    this.getMapping = this.getMapping.bind(this);
    this.getSamlconfig = this.getSamlconfig.bind(this);
    this.updateSamlconfig = this.updateSamlconfig.bind(this);
    this.deleteSamlconfig = this.deleteSamlconfig.bind(this);
    this.getProfile = this.getProfile.bind(this);
    this.getRepos = this.getRepos.bind(this);
    this.getRepoSeries = this.getRepoSeries.bind(this);
    this.getReleases = this.getReleases.bind(this);
    this.getRelease = this.getRelease.bind(this);
    this.getDeployments = this.getDeployments.bind(this);
    this.getTechnologies = this.getTechnologies.bind(this);
    this.getBps = this.getBps.bind(this);
    this.getBp = this.getBp.bind(this);
    this.searchBps = this.searchBps.bind(this);
    this.createBps = this.createBps.bind(this);
    this.deleteBps = this.deleteBps.bind(this);
    this.updateBps = this.updateBps.bind(this);
    this.sendBps = this.sendBps.bind(this);
    this.getQuestionnaires = this.getQuestionnaires.bind(this);
    this.getQuestionnaire = this.getQuestionnaire.bind(this);
    this.searchQuestionnaires = this.searchQuestionnaires.bind(this);
    this.createQuestionnaire = this.createQuestionnaire.bind(this);
    this.deleteQuestionnaire = this.deleteQuestionnaire.bind(this);
    this.updateQuestionnaire = this.updateQuestionnaire.bind(this);
    this.getTools = this.getTools.bind(this);
    this.getTool = this.getTool.bind(this);
    this.searchTools = this.searchTools.bind(this);
    this.getTags = this.getTags.bind(this);
    this.getTag = this.getTag.bind(this);
    this.searchTags = this.searchTags.bind(this);
    this.createTag = this.createTag.bind(this);
    this.deleteTag = this.deleteTag.bind(this);
    this.createQuestion = this.createQuestion.bind(this);
    this.deleteQuestion = this.deleteQuestion.bind(this);
    this.updateQuestion = this.updateQuestion.bind(this);
    this.getQuestion = this.getQuestion.bind(this);
    this.getQuestions = this.getQuestions.bind(this);
    this.getFilechanges = this.getFilechanges.bind(this);
    this.getAlerts = this.getAlerts.bind(this);
    this.getAlertsSeries = this.getAlertsSeries.bind(this);
    this.getAlertsAggregate = this.getAlertsAggregate.bind(this);
    this.getResources = this.getResources.bind(this);
    this.getAccounts = this.getAccounts.bind(this);
    this.getDevelopers = this.getDevelopers.bind(this);
    this.getQuiz = this.getQuiz.bind(this);
    this.deleteQuiz = this.deleteQuiz.bind(this);
    this.updateQuiz = this.updateQuiz.bind(this);
    this.createQuiz = this.createQuiz.bind(this);
    this.getQuizzes = this.getQuizzes.bind(this);
    this.fileUploadQuiz = this.fileUploadQuiz.bind(this);
    this.getWorkItem = this.getWorkItem.bind(this);
    this.getWorkItems = this.getWorkItems.bind(this);
    this.createWorkItem = this.createWorkItem.bind(this);
    this.deleteWorkItem = this.deleteWorkItem.bind(this);
    this.updateWorkItem = this.updateWorkItem.bind(this);
    this.getNotes = this.getNotes.bind(this);
    this.createNote = this.createNote.bind(this);
    this.createComms = this.createComms.bind(this);
    this.createCTemplate = this.createCTemplate.bind(this);
    this.deleteCTemplate = this.deleteCTemplate.bind(this);
    this.getCTemplate = this.getCTemplate.bind(this);
    this.getCTemplates = this.getCTemplates.bind(this);
    this.updateCTemplate = this.updateCTemplate.bind(this);
    this.getFile = this.getFile.bind(this);
    this.getChecklist = this.getChecklist.bind(this);
    this.getProducts = this.getProducts.bind(this);
    this.getProduct = this.getProduct.bind(this);
    this.deleteProduct = this.deleteProduct.bind(this);
    this.updateProduct = this.updateProduct.bind(this);
    this.createProduct = this.createProduct.bind(this);
    this.getGitRepos = this.getGitRepos.bind(this);
    this.getJiraProjects = this.getJiraProjects.bind(this);
    this.getActivityLogs = this.getActivityLogs.bind(this);
    this.getFields = this.getFields.bind(this);
    this.getStages = this.getStages.bind(this);
    this.updateStage = this.updateStage.bind(this);
    this.getRepositories = this.getRepositories.bind(this);
    this.getSignatureLogs = this.getSignatureLogs.bind(this);
    this.getSignatureLog = this.getSignatureLog.bind(this);
    this.getRepository = this.getRepository.bind(this);
    this.getEventLogs = this.getEventLogs.bind(this);
    this.getSignatures = this.getSignatures.bind(this);
    this.getWorkflows = this.getWorkflows.bind(this);
    this.getWorkflow = this.getWorkflow.bind(this);
    this.createWorkflow = this.createWorkflow.bind(this);
    this.deleteWorkflow = this.deleteWorkflow.bind(this);
    this.updateWorkflow = this.updateWorkflow.bind(this);
    this.getMetrics = this.getMetrics.bind(this);
  }

  getProfile() {
    return this.restInstance.get(USER_PROFILE, this.options);
  }

  getUsers(filters = { page_size: 100, page_number: 1 }) {
    let url = USERS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  createIntegration(integration) {
    // TODO hack to change type to application
    // redo the integration page to do it the right way after integration with BE
    // if(postData.hasOwnProperty("type")) {
    //     postData.application = postData.type;
    //     delete postData.type;
    // }
    // if(postData.hasOwnProperty("method") && postData.method === "form") {
    //     postData.method = "apikey";
    // }
    let postData = integration.json();
    return this.restInstance.post(INTEGRATIONS, postData, this.options);
  }

  updateIntegration(id, integration) {
    let url = INTEGRATIONS.concat("/").concat(id.toString());
    let postData = integration.json();

    // Quick fix for now , don't send keys when updating
    unset(postData, ["keys"]);
    unset(postData, ["apikey"]);

    return this.restInstance.put(url, postData, this.options);
  }

  createOAuthIntegration(integrationType, integrationName, integrationCode) {
    let postData = {
      type: integrationType,
      name: integrationName,
      code: integrationCode
    };
    return this.restInstance.post(INTEGRATION, postData, this.options);
  }

  createFormIntegration(integrationType, integrationName, formData) {
    let postData = {
      application: integrationType,
      name: integrationName,
      ...formData
    };
    return this.restInstance.post(INTEGRATION, postData, this.options);
  }

  updateFormIntegration(integrationId, formData) {
    let url = INTEGRATIONS.concat("/").concat(integrationId);
    return this.restInstance.put(url, formData, this.options);
  }

  getIntegration(integrationId) {
    let url = INTEGRATIONS.concat("/").concat(integrationId);
    return this.restInstance.get(url, this.options);
  }

  getIntegrations(filter = {}) {
    let url = INTEGRATIONS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  deleteIntegration(integrationId) {
    let url = INTEGRATIONS.concat("/").concat(integrationId);
    return this.restInstance.delete(url, this.options);
  }

  getDashboard(dashboardId) {
    let url = DASHBOARDS.concat("/").concat(dashboardId.toString());
    return this.restInstance.get(url, this.options);
  }

  getDashboards(filters = {}) {
    let url = DASHBOARDS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getQuery(query) {
    let postData = {
      query: query
    };
    return this.restInstance.post(QUERY, postData, this.options);
  }

  getNotifications(filter = {}) {
    // by default get the 5 newest notifications over the last 30 minutes
    let url = NOTIFICATIONS.concat("/list");
    console.log(url);
    return this.restInstance.post(url, filter, this.options);
  }

  getRbacs(filters = { page_number: 1, page_size: 100 }) {
    let url = RBACS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  addUser(user) {
    let postData = user.json();
    return this.restInstance.post(USERS, postData, this.options);
  }

  getUser(id) {
    let url = USERS.concat("/").concat(id.toString());
    return this.restInstance.get(url, this.options);
  }

  updateUser(id, user) {
    let postData = user.json();
    let url = USERS.concat("/").concat(id.toString());
    return this.restInstance.put(url, postData, this.options);
  }

  deleteUser(id) {
    let url = USERS.concat("/").concat(id.toString());
    return this.restInstance.delete(url, this.options);
  }

  updateRbac(id, name, permissions) {
    let postData = {
      id: id,
      name: name,
      permissions: permissions
    };
    let url = RBAC.concat("/").concat(id.toString());
    return this.restInstance.put(url, postData, this.options);
  }

  getOrganizations(filters = { page_number: 1, page_size: 100 }) {
    let url = ORGS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getOrganization(orgId) {
    let url = ORGS.concat("/").concat(orgId.toString());
    return this.restInstance.get(url, this.options);
  }

  createOrganization(org) {
    // let postData={
    //     name: name
    // };
    let postData = org.json();
    return this.restInstance.post(ORGS, postData, this.options);
  }

  deleteOrganization(orgId) {
    let url = ORGS.concat(`/${orgId}`);
    return this.restInstance.delete(url, this.options);
  }

  getTeams(filters = { page_number: 1, page_size: 100, org_id: null }) {
    let url = TEAMS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getTeam(teamId) {
    let url = TEAMS.concat("/").concat(teamId.toString());
    return this.restInstance.get(url, this.options);
  }

  createTeam(team) {
    let postData = team.json();
    return this.restInstance.post(TEAMS, postData, this.options);
  }

  updateTeam(teamId, team) {
    // let postData = {
    //     name: name,
    //     organization_id:organizationId
    // };
    let url = TEAMS.concat("/").concat(teamId.toString());
    let postData = team.json();
    return this.restInstance.put(url, postData, this.options);
  }

  deleteTeam(teamId) {
    let url = TEAMS.concat(`/${teamId}`);
    return this.restInstance.delete(url, this.options);
  }

  // getMappings(filters={page_number:0,page_size:100,filter:{team_id:null}}) {
  //     let url = TEAM_MAPPINGS.concat("/list");
  //
  //     return (this.restInstance.post(url,filters,this.options))
  // }
  //
  // createMapping(mapping) {
  //     // let postData = {
  //     //     integration_id: integrationId,
  //     //     selected: selected,
  //     //     team_id: teamId
  //     // };
  //     console.log(mapping);
  //     let postData = mapping.json();
  //     console.log("post json");
  //     return(this.restInstance.post(TEAM_MAPPINGS,postData,this.options));
  // }
  //
  // updateMapping(mappingId,mapping) {
  //     let url = TEAM_MAPPINGS.concat(`/${mappingId}`);
  //     // let postData = {
  //     //     selected: selected,
  //     // };
  //     let postData = mapping.json();
  //     return(this.restInstance.put(url,postData,this.options))
  // }
  //
  // deleteMapping(mappingId) {
  //     let url = TEAM_MAPPINGS.concat(`/${mappingId}`)
  //     return(this.restInstance.delete(url,this.options));
  // }

  getSamlconfig(id = 0) {
    return this.restInstance.get(SAML_CONFIG, this.options);
  }

  updateSamlconfig(id = 0, samlConfig) {
    let postData = samlConfig.json();
    return this.restInstance.put(SAML_CONFIG, postData, this.options);
  }

  deleteSamlconfig(id = 0) {
    return this.restInstance.delete(SAML_CONFIG, this.options);
  }

  getRepos(filters) {
    let url = REPOS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getRepoSeries(filters) {
    let url = REPOS.concat("/series");
    return this.restInstance.post(url, filters, this.options);
  }

  getDeployments(filters) {
    let url = DEPLOYMENTS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getTechnologies(filters) {
    let url = TECHNOLOGIES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getReleases(filters) {
    let url = RELEASES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getRelease(id) {
    let url = RELEASES.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  getBps(filters) {
    let url = BPS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getBp(id) {
    let url = BPS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  searchBps(filters) {
    let url = BPS.concat("/search");
    return this.restInstance.post(url, filters, this.options);
  }

  createBps(bp) {
    let postData = bp.json();
    return this.restInstance.post(BPS, postData, this.options);
  }

  deleteBps(id) {
    let url = BPS.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  updateBps(id, bps) {
    let url = BPS.concat(`/${id}`);
    let postData = bps.json();
    return this.restInstance.put(url, postData, this.options);
  }

  sendBps(bps) {
    let url = BPS.concat(`/send`);
    let postData = bps.json();
    return this.restInstance.post(url, postData, this.options);
  }

  getTools(filters) {
    let url = TOOLS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getTool(id) {
    let url = TOOLS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  searchTools(filters) {
    let url = TOOLS.concat("/search");
    return this.restInstance.post(url, filters, this.options);
  }

  getQuestionnaires(filters) {
    let url = QUESTIONNAIRES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getQuestionnaire(id) {
    let url = QUESTIONNAIRES.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  searchQuestionnaires(filters) {
    let url = QUESTIONNAIRES.concat("/search");
    return this.restInstance.post(url, filters, this.options);
  }

  createQuestionnaire(questionnaire) {
    let postData = questionnaire.json();
    return this.restInstance.post(QUESTIONNAIRES, postData, this.options);
  }

  deleteQuestionnaire(id) {
    let url = QUESTIONNAIRES.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  updateQuestionnaire(id, questionnaire) {
    let url = QUESTIONNAIRES.concat(`/${id}`);
    let postData = questionnaire.json();
    return this.restInstance.put(url, postData, this.options);
  }

  getTags(filters) {
    let url = TAGS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getTag(id) {
    let url = TAGS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  searchTags(filters) {
    let url = TAGS.concat("/search");
    return this.restInstance.post(url, filters, this.options);
  }

  createTag(tag) {
    let postData = tag.json();
    return this.restInstance.post(TAGS, postData, this.options);
  }

  deleteTag(id) {
    let url = TAGS.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  createQuestion(question) {
    let postData = question.json();
    return this.restInstance.post(SECTIONS, postData, this.options);
  }

  updateQuestion(questionId, question) {
    let url = SECTIONS.concat(`/${questionId}`);
    let postData = question.json();
    return this.restInstance.put(url, postData, this.options);
  }

  deleteQuestion(questionId) {
    let url = SECTIONS.concat(`/${questionId}`);
    return this.restInstance.delete(url, this.options);
  }

  getQuestion(questionId) {
    let url = SECTIONS.concat(`/${questionId}`);
    return this.restInstance.get(url, this.options);
  }

  getQuestions(filters) {
    let url = SECTIONS.concat(`/list`);
    return this.restInstance.post(url, filters, this.options);
  }

  getFilechanges(filters) {
    let url = FILECHANGES.concat(`/list`);
    return this.restInstance.post(url, filters, this.options);
  }

  getAlerts(filters) {
    let url = ALERTS.concat(`/list`);
    return this.restInstance.post(url, filters, this.options);
  }

  getAlertsSeries(filters) {
    let url = ALERTS.concat(`/series`);
    return this.restInstance.post(url, filters, this.options);
  }

  getAlertsAggregate(filters) {
    let url = ALERTS.concat(`/aggregate`);
    return this.restInstance.post(url, filters, this.options);
  }

  getResources(filters) {
    let url = RESOURCES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getAccounts(filters) {
    let url = ACCOUNTS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getDevelopers(filters) {
    let url = DEVELOPERS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getQuiz(quizId) {
    let url = QUIZ.concat(`/${quizId}`);
    return this.restInstance.get(url, this.options);
  }

  deleteQuiz(quizId) {
    let url = QUIZ.concat(`/${quizId}`);
    return this.restInstance.delete(url, this.options);
  }

  updateQuiz(quizId, quiz) {
    let url = QUIZ.concat(`/${quizId}`);
    let postData = quiz.json();
    return this.restInstance.put(url, postData, this.options);
  }

  createQuiz(quiz) {
    let postData = quiz.json();
    return this.restInstance.post(QUIZ, postData, this.options);
  }

  getQuizzes(filters) {
    let url = QUIZ.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  fileUploadQuiz(id, file) {
    let [quizId, assertionId] = id.split(":");
    let url = FILE_UPLOAD.concat(`/quiz/${quizId}/assertion/${assertionId}`);
    let formData = new FormData();
    formData.append("file", file);
    let options = this.options;
    options.headers = { "Content-Type": "multipart/formdata" };
    return this.restInstance.post(url, formData, options);
  }

  getWorkItem(id) {
    let url = WORK_ITEM.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  getWorkItems(filters) {
    let url = WORK_ITEM.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  createWorkItem(workItem) {
    let postData = workItem.json();
    return this.restInstance.post(WORK_ITEM, postData, this.options);
  }

  createBlankWorkItem = data => {
    return this.restInstance.post(WORK_ITEM, data, this.options);
  };

  deleteWorkItem(id) {
    let url = WORK_ITEM.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  updateWorkItem(id, workItem) {
    let url = WORK_ITEM.concat(`/${id}`);
    let postData = workItem.json();
    return this.restInstance.put(url, postData, this.options);
  }

  getNotes(filters) {
    let url = NOTES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  createNote(note) {
    let postData = note.json();
    return this.restInstance.post(NOTES, postData, this.options);
  }

  createComms(comms) {
    let postData = comms.json();
    return this.restInstance.post(COMMS, postData, this.options);
  }

  createCTemplate(template) {
    let postData = template.json();
    return this.restInstance.post(CTEMPLATE, postData, this.options);
  }

  deleteCTemplate(id) {
    let url = CTEMPLATE.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  getCTemplate(id) {
    let url = CTEMPLATE.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  updateCTemplate(id, template) {
    let url = CTEMPLATE.concat(`/${id}`);
    let postData = template.json();
    return this.restInstance.put(url, postData, this.options);
  }

  getCTemplates(filters) {
    let url = CTEMPLATE.concat(`/list`);
    return this.restInstance.post(url, filters, this.options);
  }

  getFile(id) {
    let options = { responseType: "blob" };
    let url = FILE_UPLOAD.concat(`/${id}`);
    return this.restInstance.get(url, options);
  }

  getChecklist(id) {
    let url = CHECKLISTS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  getProducts(filters) {
    let url = PRODUCTS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getProduct(id) {
    let url = PRODUCTS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  deleteProduct(id) {
    let url = PRODUCTS.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  updateProduct(id, product) {
    let url = PRODUCTS.concat(`/${id}`);
    //let postData = product.json();
    let postData = product.post_data;
    return this.restInstance.put(url, postData, this.options);
  }

  createProduct(product) {
    //let postData = product.json();
    let postData = product.post_data;
    return this.restInstance.post(PRODUCTS, postData, this.options);
  }

  getGitRepos(filters) {
    let url = GITREPOS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getJiraProjects(filters) {
    let url = JIRAPROJECTS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getActivityLogs(filters) {
    let url = ACTIVITYLOGS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getFields(filters) {
    let url = FIELDS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getStages(filters) {
    let url = STAGES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  updateStage(id, stage) {
    let url = STAGES.concat(`/${id}`);
    let postData = stage.json();
    return this.restInstance.put(url, postData, this.options);
  }

  getMappings(filters) {
    let url = MAPPINGS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getMapping(id) {
    let url = MAPPINGS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  deleteMapping(id) {
    let url = MAPPINGS.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  updateMapping(id, mapping) {
    let url = MAPPINGS.concat(`/${id}`);
    let postData = mapping.json();
    return this.restInstance.put(url, postData, this.options);
  }

  createMapping(mapping) {
    let postData = mapping.json();
    return this.restInstance.post(MAPPINGS, postData, this.options);
  }

  getRepositories(filters) {
    let url = REPOSITORIES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getSignatureLogs(filters) {
    let url = SIGNATURE_LOGS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getSignatureLog(id) {
    let url = SIGNATURE_LOGS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }
  getRepository(id) {
    let url = REPOSITORIES.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  getEventLogs(filters) {
    const url = EVENT_LOGS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getSignatures(filters) {
    const url = SIGNATURES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getWorkflows(filters) {
    let url = WORKFLOWS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  getWorkflow(id) {
    let url = WORKFLOWS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  deleteWorkflow(id) {
    let url = WORKFLOWS.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  updateWorkflow(id, mapping) {
    let url = WORKFLOWS.concat(`/${id}`);
    let postData = mapping.json();
    return this.restInstance.put(url, postData, this.options);
  }

  createWorkflow(mapping) {
    let postData = mapping.json();
    return this.restInstance.post(WORKFLOWS, postData, this.options);
  }

  getMetrics(filters) {
    const url = METRICS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }
}
