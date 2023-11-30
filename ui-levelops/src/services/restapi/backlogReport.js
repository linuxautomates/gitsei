import BackendService from "./backendService";
import { BACKLOG_REPORT } from "constants/restUri";

export class BackogReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BACKLOG_REPORT, filter, this.options);
  }
}
