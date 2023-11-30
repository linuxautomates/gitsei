import BackendService from "./backendService";
import { CONFIGURE_DASHBOARD } from "constants/restUri";

export class ConfigureDashService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = CONFIGURE_DASHBOARD.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  create(item) {
    let postData = item.json;
    return this.restInstance.post(CONFIGURE_DASHBOARD, postData, this.options);
  }
}
