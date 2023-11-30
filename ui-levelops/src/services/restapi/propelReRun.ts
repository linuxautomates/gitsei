import { PROPEL_RUN } from "constants/restUri";
import BackendService from "./backendService";

export class RestPropelReRun extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get(id: string) {
    let url = `${PROPEL_RUN}/${id}/retry`;
    return this.restInstance.post(url, {}, this.options);
  }
}
