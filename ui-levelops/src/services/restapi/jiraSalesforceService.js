import BackendService from "./backendService";
import {
  JIRA_SALESFORCE_AGGS,
  JIRA_SALESFORCE_AGGS_LIST_JIRA,
  JIRA_SALESFORCE_AGGS_LIST_SALESFORCE,
  JIRA_SALESFORCE_AGGS_LIST_COMMIT,
  JIRA_SALESFORCE_ESCALATION_TIME_REPORT,
  JIRA_SALESFORCE_FILES,
  JIRA_SALESFORCE_RESOLVED_TICKETS_TREND,
  JIRA_SALESFORCE_FILES_REPORT
} from "../../constants/restUri";

export class JiraSalesforceService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_SALESFORCE_AGGS, filter, this.options);
  }
}

export class JiraSalesforceAggsListJiraService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_SALESFORCE_AGGS_LIST_JIRA, filter, this.options);
  }
}

export class JiraSalesforceAggsListSalesforceService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_SALESFORCE_AGGS_LIST_SALESFORCE, filter, this.options);
  }
}

export class JiraSalesforceAggsListCommitService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_SALESFORCE_AGGS_LIST_COMMIT, filter, this.options);
  }
}

export class JiraSalesforceEscalationTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_SALESFORCE_ESCALATION_TIME_REPORT, filter, this.options);
  }
}

export class JiraSalesforceFilesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = `${JIRA_SALESFORCE_FILES}/list`;
    return this.restInstance.post(uri, filter, this.options);
  }
}

export class JiraSalesforceFilesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_SALESFORCE_FILES_REPORT, filter, this.options);
  }
}

export class JiraSalesforceResolvedTicketsTrendService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_SALESFORCE_RESOLVED_TICKETS_TREND, filter, this.options);
  }
}
