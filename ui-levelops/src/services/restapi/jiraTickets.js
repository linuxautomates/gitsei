import BackendService from "./backendService";
import { JIRA_ISSUES } from "constants/restUri";

export class JiraTicketsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_ISSUES.concat("/list"), filter, this.options);
  }
}
