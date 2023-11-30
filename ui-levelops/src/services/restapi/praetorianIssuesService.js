import BackendService from "./backendService";
import { PRAETORIAN_ISSUES_AGGS, PRAETORIAN_ISSUES_LIST, PRAETORIAN_ISSUES_VALUES } from "../../constants/restUri";

export class PraetorianIssuesListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(PRAETORIAN_ISSUES_LIST, filter, this.options);
  }
}

export class PraetorianIssuesValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(PRAETORIAN_ISSUES_VALUES, filter, this.options);
  }
}

export class PraetorianIssuesAggsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(PRAETORIAN_ISSUES_AGGS, filter, this.options);
  }
}
