import BackendService from "./backendService";
import { HYGIENE_REPORT } from "constants/restUri";

export class HygieneReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(HYGIENE_REPORT, filter, this.options);
  }
}
