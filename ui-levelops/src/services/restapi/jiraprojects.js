import BackendService from "./backendService";
import { JIRAPROJECTS } from "constants/restUri";

export class RestJiraprojectsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = JIRAPROJECTS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
