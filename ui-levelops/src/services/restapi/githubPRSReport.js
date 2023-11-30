import BackendService from "./backendService";
import { GITHUB_PRS_REPORT } from "constants/restUri";

export class GithubPRSReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(GITHUB_PRS_REPORT, filter, this.options);
  }
}
