import BackendService from "services/backendService";
import { TEAM_ALLOCATION_REPORT } from "constants/restUri";

export class TeamAllocationReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(TEAM_ALLOCATION_REPORT, filters, this.options);
  }
}
