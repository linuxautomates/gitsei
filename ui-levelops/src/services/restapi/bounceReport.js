import BackendService from "./backendService";
import { BOUNCE_REPORT, ISSUE_MANAGEMENT_BOUNCE_REPORT } from "constants/restUri";

export class BounceReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BOUNCE_REPORT, filter, this.options);
  }
}

export class IssueManagementBounceReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_BOUNCE_REPORT, filter, this.options);
  }
}
