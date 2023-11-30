import BackendService from "./backendService";
import { PLUGIN_AGGS } from "constants/restUri";

export class RestPluginAggsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = PLUGIN_AGGS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = PLUGIN_AGGS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}
