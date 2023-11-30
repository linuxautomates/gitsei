import BackendService from "./backendService";
import { REPORT_DOCS, REPORT_DOCS_LIST } from "../../constants/restUri";

export class ReportsDocsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(REPORT_DOCS_LIST, filter, this.options);
  }

  get(id) {
    const url = `${REPORT_DOCS}/${id}`;
    return this.restInstance.get(url, this.options);
  }
}
