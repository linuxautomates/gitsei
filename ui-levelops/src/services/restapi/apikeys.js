import BackendService from "./backendService";
import { APIKEYS } from "constants/restUri";

export class RestApikeysService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.create = this.create.bind(this);
    this.delete = this.delete.bind(this);
  }

  list(filter = {}) {
    let url = APIKEYS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  create(item) {
    let postData = item.json;
    return this.restInstance.post(APIKEYS, postData, this.options);
  }

  delete(id) {
    let url = APIKEYS.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }
}
