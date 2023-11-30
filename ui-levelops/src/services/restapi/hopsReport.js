import BackendService from "./backendService";
import { HOPS_REPORT, ISSUE_MANAGEMENT_HOPS_REPORT } from "constants/restUri";

export class HopsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(HOPS_REPORT, filter, this.options);
  }
}

export class IssueManagementHopsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_HOPS_REPORT, filter, this.options);
  }
}
