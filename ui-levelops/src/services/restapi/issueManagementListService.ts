import BackendService from "./backendService";
import { ISSUE_MANAGEMENT } from "../../constants/restUri";

export class IssueManagementWorkItemListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = `${ISSUE_MANAGEMENT}/list`;
    return this.restInstance.post(uri, filter, this.options);
  }
}
