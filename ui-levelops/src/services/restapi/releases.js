import BackendService from "../backendService";
import { RELEASES } from "constants/restUri";

export class RestReleasesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = RELEASES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = RELEASES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}
