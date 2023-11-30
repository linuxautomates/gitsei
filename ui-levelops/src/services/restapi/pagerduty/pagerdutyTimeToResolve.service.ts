import BackendService from "../backendService";
import { PAGERDUTY_RESOLUTION_TIME_REPORT, PAGERDUTY_RESPONSE_TIME_REPORT } from "constants/restUri";

export class PagerdutyResolutionTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(PAGERDUTY_RESOLUTION_TIME_REPORT, filters, this.options);
  }
}

export class PagerdutyResponseTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(PAGERDUTY_RESPONSE_TIME_REPORT, filters, this.options);
  }
}
