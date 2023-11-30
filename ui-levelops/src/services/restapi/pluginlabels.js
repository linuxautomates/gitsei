import BackendService from "./backendService";
import { PLUGIN_LABELS } from "constants/restUri";

export class RestPluginLabelsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.values = this.values.bind(this);
  }

  list(filter = {}) {
    let url = PLUGIN_LABELS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  values(id, filter = {}) {
    let url = PLUGIN_LABELS.concat(`/${id}/values`);
    return this.restInstance.post(url, filter, this.options);
  }
}
