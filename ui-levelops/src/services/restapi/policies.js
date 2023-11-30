import BackendService from "./backendService";
import { POLICIES } from "constants/restUri";

export class RestPoliciesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = POLICIES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = POLICIES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}
