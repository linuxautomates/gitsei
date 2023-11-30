import BackendService from "./backendService";
import {
  JIRA_ZENDESK_AGGS,
  JIRA_ZENDESK_AGGS_LIST_JIRA,
  JIRA_ZENDESK_AGGS_LIST_ZENDESK,
  JIRA_ZENDESK_AGGS_LIST_COMMIT,
  JIRA_ZENDESK_ESCALATION_TIME_REPORT,
  JIRA_ZENDESK_FILES,
  JIRA_ZENDESK_RESOLVED_TICKETS_TREND,
  JIRA_ZENDESK_FILES_REPORT
} from "../../constants/restUri";

export class JiraZendeskService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_ZENDESK_AGGS, filter, this.options);
  }
}

export class JiraZendeskAggsListJiraService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_ZENDESK_AGGS_LIST_JIRA, filter, this.options);
  }
}

export class JiraZendeskAggsListZendeskService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_ZENDESK_AGGS_LIST_ZENDESK, filter, this.options);
  }
}

export class JiraZendeskAggsListCommitService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_ZENDESK_AGGS_LIST_COMMIT, filter, this.options);
  }
}

export class JiraZendeskEscalationTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_ZENDESK_ESCALATION_TIME_REPORT, filter, this.options);
  }
}

export class JiraZendeskFilesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = `${JIRA_ZENDESK_FILES}/list`;
    return this.restInstance.post(uri, filter, this.options);
  }
}

export class JiraZendeskFilesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_ZENDESK_FILES_REPORT, filter, this.options);
  }
}

export class JiraZendeskResolvedTicketsTrendService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_ZENDESK_RESOLVED_TICKETS_TREND, filter, this.options);
  }
}
