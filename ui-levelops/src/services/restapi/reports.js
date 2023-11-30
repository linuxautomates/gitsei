import BackendService from "./backendService";
import { REPORTS } from "constants/restUri";

export class RestReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = REPORTS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = REPORTS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}
