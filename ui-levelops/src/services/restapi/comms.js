import BackendService from "./backendService";
import { COMMS } from "constants/restUri";

export class RestCommsService extends BackendService {
  constructor() {
    super();
    this.create = this.create.bind(this);
  }

  create(comms) {
    let postData = comms.json();
    return this.restInstance.post(COMMS, postData, this.options);
  }
}
