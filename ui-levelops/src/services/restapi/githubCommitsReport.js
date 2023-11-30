import BackendService from "./backendService";
import { GITHUB_COMMITS_REPORT } from "constants/restUri";

export class GithubCommitsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(GITHUB_COMMITS_REPORT, filter, this.options);
  }
}
