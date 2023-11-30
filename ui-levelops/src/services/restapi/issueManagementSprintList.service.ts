import BackendService from "./backendService";
import { ISSUE_MANAGEMENT_SPRINTS } from "../../constants/restUri";

export class IssueManagementSprintListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = `${ISSUE_MANAGEMENT_SPRINTS}/list`;
    return this.restInstance.post(uri, filter, this.options);
  }
}
