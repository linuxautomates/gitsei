import { CICD_JOB_PARAMS } from "constants/restUri";
import BackendService from "./backendService";

export class JobParamsService extends BackendService {
  constructor() {
    super();
    this.getParams = this.getParams.bind(this);
  }

  getParams(filter = {}) {
    return this.restInstance.post(CICD_JOB_PARAMS, filter, this.options);
  }
}
