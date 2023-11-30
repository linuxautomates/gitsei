import BackendService from "./backendService";
import { SCM_REWORK_REPORT } from "constants/restUri";

export class SCMReworkReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_REWORK_REPORT, filter, this.options);
  }
}
