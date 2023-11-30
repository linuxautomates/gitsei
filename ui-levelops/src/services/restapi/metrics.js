import BackendService from "./backendService";
import { METRICS } from "constants/restUri";

export class RestMetricsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = METRICS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
