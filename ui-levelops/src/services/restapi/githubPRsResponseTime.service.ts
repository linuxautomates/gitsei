import BackendService from "./backendService";
import { SCM_PRS_AUTHOR_RESPONSE_TIME, SCM_PRS_REVIEWER_RESPONSE_TIME } from "constants/restUri";

export class GithubPRsAuthorResponseTimeService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_PRS_AUTHOR_RESPONSE_TIME, filter, this.options);
  }
}

export class GithubPRsReviewerResponseTimeService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_PRS_REVIEWER_RESPONSE_TIME, filter, this.options);
  }
}
