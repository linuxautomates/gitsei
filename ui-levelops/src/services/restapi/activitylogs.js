import BackendService from "./backendService";
import { ACTIVITYLOGS } from "constants/restUri";

export class RestActivitylogsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = ACTIVITYLOGS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
