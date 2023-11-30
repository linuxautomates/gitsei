import BackendService from "./backendService";
import { RESOLUTION_TIME_REPORT } from "constants/restUri";

export class ResolutionTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(RESOLUTION_TIME_REPORT, filter, this.options);
  }
}
