import BackendService from "./backendService";
import { CONTENT_SCHEMA } from "constants/restUri";

export class RestContentSchema extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = CONTENT_SCHEMA.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
