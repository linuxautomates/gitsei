import BackendService from "./backendService";
import { TIME_ACROSS_STAGES_REPORT } from "constants/restUri";

export class JiraTimeAcrossStagesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(TIME_ACROSS_STAGES_REPORT, filter, this.options);
  }
}
