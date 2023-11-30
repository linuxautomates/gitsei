import BackendService from "./backendService";
import { GITHUB_CODING_DAY, GITHUB_COMMITS_PER_CODING_DAY } from "constants/restUri";

export class GithubCodingDayReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(GITHUB_CODING_DAY, filter, this.options);
  }
}

export class GithubCommitsPerCodingDayReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(GITHUB_COMMITS_PER_CODING_DAY, filter, this.options);
  }
}
