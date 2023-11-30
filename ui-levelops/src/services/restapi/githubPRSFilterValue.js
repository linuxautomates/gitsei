import BackendService from "./backendService";
import { GITHUB_PRS_FILTER_VALUES } from "constants/restUri";

export class GithubPRSFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(GITHUB_PRS_FILTER_VALUES, filter, this.options);
  }
}
