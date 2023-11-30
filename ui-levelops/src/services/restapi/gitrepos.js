import BackendService from "./backendService";
import { GITREPOS } from "constants/restUri";

export class RestGitreposService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = GITREPOS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
