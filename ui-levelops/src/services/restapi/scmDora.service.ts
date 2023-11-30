import { DORA_SCM_FAILURE_RATE } from "constants/restUri";
import BackendService from "services/backendService";

export class SCMDoraFailureRateService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(DORA_SCM_FAILURE_RATE, filters, this.options);
  }
}
