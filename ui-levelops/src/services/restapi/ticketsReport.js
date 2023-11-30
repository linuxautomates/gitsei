import BackendService from "./backendService";
import { TICKETS_REPORT, ASSIGNEE_TIME_REPORT, FIRST_ASSIGNEE_REPORT } from "constants/restUri";
import { ISSUE_MANAGEMENT_FIRST_ASSIGNEE_REPORT } from "../../constants/restUri";

export class TicketsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(TICKETS_REPORT, filter, this.options);
  }
}

export class AssigneeTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ASSIGNEE_TIME_REPORT, filter, this.options);
  }
}

export class FirstAssigneeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(FIRST_ASSIGNEE_REPORT, filter, this.options);
  }
}

export class IssueManagementFirstAssigneeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_FIRST_ASSIGNEE_REPORT, filter, this.options);
  }
}
