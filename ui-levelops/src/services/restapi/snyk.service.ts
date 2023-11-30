import { SNYK_ISSUES_LIST, SNYK_ISSUES_REPORT, SNYK_ISSUES_VALUES } from "constants/restUri";
import BackendService from "services/backendService";

export class SnykIssuesListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SNYK_ISSUES_LIST, filter, this.options);
  }
}

export class SnykIssuesFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SNYK_ISSUES_VALUES, filter, this.options);
  }
}

export class SnykIssuesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SNYK_ISSUES_REPORT, filter, this.options);
  }
}
