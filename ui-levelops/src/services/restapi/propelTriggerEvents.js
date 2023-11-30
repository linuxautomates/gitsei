import BackendService from "./backendService";
import { PROPEL_TRIGGER_EVENTS } from "constants/restUri";

export class RestPropelTriggerEvents extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = PROPEL_TRIGGER_EVENTS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = PROPEL_TRIGGER_EVENTS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}
