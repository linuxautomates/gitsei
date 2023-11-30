import BackendService from "./backendService";
import { SCM_RESOLUTION_TIME_REPORT } from "constants/restUri";

export class SCMResolutionTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_RESOLUTION_TIME_REPORT, filter, this.options);
  }
}
