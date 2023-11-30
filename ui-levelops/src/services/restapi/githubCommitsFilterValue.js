import BackendService from "./backendService";
import { GITHUB_COMMITS_FILTER_VALUES } from "constants/restUri";

export class GithubCommitsFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(GITHUB_COMMITS_FILTER_VALUES, filter, this.options);
  }
}
