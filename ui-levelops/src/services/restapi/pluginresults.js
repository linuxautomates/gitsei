import BackendService from "./backendService";
import { PLUGIN_RESULTS } from "constants/restUri";

export class RestPluginresultsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.diff = this.diff.bind(this);
    this.update = this.update.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = PLUGIN_RESULTS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = PLUGIN_RESULTS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  update(id, data) {
    let url = PLUGIN_RESULTS.concat("/").concat(id.toString());
    return this.restInstance.put(url, data, this.options);
  }

  diff(before = undefined, after = undefined) {
    let url = PLUGIN_RESULTS.concat("/diff");
    let options = this.options;
    options.params = {
      before_id: before,
      after_id: after
    };
    return this.restInstance.get(url, options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(PLUGIN_RESULTS, { ...this.options, data: ids });
  }
}
