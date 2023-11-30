import BackendService from "../backendService";
import { SIGNATURE_LOGS } from "constants/restUri";

export class RestSignaturelogsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = SIGNATURE_LOGS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = SIGNATURE_LOGS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}
