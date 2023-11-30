import BackendService from "./backendService";
import { RESPONSE_TIME_REPORT } from "constants/restUri";

export class ResponseTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(RESPONSE_TIME_REPORT, filter, this.options);
  }
}
