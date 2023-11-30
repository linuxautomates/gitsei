import BackendService from "./backendService";
import { EVENT_LOGS } from "constants/restUri";

export class RestEventlogsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = EVENT_LOGS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
