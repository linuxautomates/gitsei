import { PROPEL_RUNS } from "constants/restUri";
import BackendService from "./backendService";

export class RestPropelRuns extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = PROPEL_RUNS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = PROPEL_RUNS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(PROPEL_RUNS, { ...this.options, data: ids });
  }
}
