import BackendService from "./backendService";
import { PROPEL_REPORTS } from "constants/restUri";

export class RestPropelReports extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = PROPEL_REPORTS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = PROPEL_REPORTS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(PROPEL_REPORTS, { ...this.options, data: ids });
  }
}
