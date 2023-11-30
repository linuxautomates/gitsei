import BackendService from "./backendService";
import { SCM_ISSUE_REPORT, SCM_ISSUES, SCM_ISSUES_FILTER_VALUES, SCM_ISSUE_FIRST_RESPONSE } from "constants/restUri";

export class SCMIssuesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_ISSUE_REPORT, filter, this.options);
  }
}

export class SCMIssuesFilterValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_ISSUES_FILTER_VALUES, filter, this.options);
  }
}

export class SCMIssuesTicketsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_ISSUES.concat("/list"), filter, this.options);
  }
}

export class SCMIssuesFirstResponseService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_ISSUE_FIRST_RESPONSE, filter, this.options);
  }
}
