import BackendService from "./backendService";
import { NCC_GROUP_ISSUES_AGGS, NCC_GROUP_ISSUES_LIST, NCC_GROUP_ISSUES_VALUES } from "../../constants/restUri";

export class NccGroupIssuesListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(NCC_GROUP_ISSUES_LIST, filter, this.options);
  }
}

export class NccGroupIssuesValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(NCC_GROUP_ISSUES_VALUES, filter, this.options);
  }
}

export class NccGroupIssuesAggsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(NCC_GROUP_ISSUES_AGGS, filter, this.options);
  }
}
