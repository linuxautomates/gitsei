import BackendService from "../backendService";
import { SPRINT_REPORT, JIRA_SRPINTS } from "../../constants/restUri";

export class JiraSprintService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SPRINT_REPORT, filter, this.options);
  }
}

export class JiraSprintFilterService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const url = `${JIRA_SRPINTS}/list`;
    return this.restInstance.post(url, filter, this.options);
  }
}
