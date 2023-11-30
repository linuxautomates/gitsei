import BackendService from "./backendService";
import { GITHUB_COMMITS } from "constants/restUri";

export class GithubCommitsTicketsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(GITHUB_COMMITS.concat("/list"), filter, this.options);
  }
}
