import BackendService from "./backendService";
import { JIRA_STAGE_BOUNCE_REPORT, ISSUE_MANAGEMENT_STAGE_BOUNCE_REPORT } from "constants/restUri";

export class JiraStageBounceReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_STAGE_BOUNCE_REPORT, filter, this.options);
  }
}

export class AzureStageBounceReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_STAGE_BOUNCE_REPORT, filter, this.options);
  }
}
