import BackendService from "./backendService";
import {
  SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND,
  SCM_PRS_FIRST_REVIEW_TREND,
  SCM_PRS_MERGE_TREND
} from "constants/restUri";

export class SCMPRSMergeTrendReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_PRS_MERGE_TREND, filter, this.options);
  }
}

export class SCMPRSFirstReviewTrendService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_PRS_FIRST_REVIEW_TREND, filter, this.options);
  }
}

export class SCMPRSFirstReviewToMergeTrendService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND, filter, this.options);
  }
}
