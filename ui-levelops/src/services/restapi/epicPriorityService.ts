import { EPIC_PRIORITY_TREND_REPORT } from "constants/restUri";
import BackendService from "services/backendService";

export class EpicPriorityReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(EPIC_PRIORITY_TREND_REPORT, filter, this.options);
  }
}
