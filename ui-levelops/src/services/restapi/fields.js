import BackendService from "./backendService";
import { FIELDS } from "constants/restUri";

export class RestFieldsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = FIELDS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
