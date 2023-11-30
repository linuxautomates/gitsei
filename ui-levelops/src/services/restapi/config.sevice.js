import BackendService from "./backendService";
import { CONFIGS } from "constants/restUri";

export class ConfigService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    let url = CONFIGS.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  update = (id, config) => {
    return this.restInstance.post(CONFIGS, config, this.options);
  };
}
