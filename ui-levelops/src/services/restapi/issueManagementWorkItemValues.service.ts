import BackendService from "./backendService";
import { ISSUE_MANAGEMENT_CUSTOM_FIELD_VALUES, ISSUE_MANAGEMENT_WORKITEM } from "../../constants/restUri";

export class IssueManagementWorkItemValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_WORKITEM, filter, this.options);
  }
}

export class IssueManagementCustomFieldValues extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_CUSTOM_FIELD_VALUES, filter, this.options);
  }
}
