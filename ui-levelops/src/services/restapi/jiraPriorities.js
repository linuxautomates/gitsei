import BackendService from "./backendService";
import { JIRA_PRIORITIES } from "constants/restUri";

export class RestJiraPrioritiesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.update = this.update.bind(this);
  }

  list(filter = {}) {
    let url = JIRA_PRIORITIES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  update(id, bulkPriorities) {
    let url = JIRA_PRIORITIES.concat("/bulk");
    return this.restInstance.put(url, bulkPriorities, this.options);
  }
}
