import BackendService from "./backendService";
import { GITHUB_CARDS, GITHUB_TIME_ACROSS_STAGES } from "../../constants/restUri";

export class SCMIssuesTimeAcrossStagesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(GITHUB_TIME_ACROSS_STAGES, filters, this.options);
  }
}

export class SCMIssuesTimeAcrossStagesFilterValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${GITHUB_CARDS}/values`, filters, this.options);
  }
}

export class GithubCardsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${GITHUB_CARDS}/list`, filters, this.options);
  }
}
