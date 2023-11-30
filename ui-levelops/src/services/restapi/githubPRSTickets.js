import BackendService from "./backendService";
import { GITHUB_PRS } from "constants/restUri";

export class GithubPRSTicketsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(GITHUB_PRS.concat("/list"), filter, this.options);
  }
}
