import BackendService from "./backendService";
import { SIGNATURES } from "constants/restUri";

export class RestSignaturesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = SIGNATURES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
