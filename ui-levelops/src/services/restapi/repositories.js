import BackendService from "./backendService";
import { REPOSITORIES } from "constants/restUri";

export class RestRepositoriesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = REPOSITORIES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = REPOSITORIES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}
