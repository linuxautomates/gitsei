import BackendService from "../backendService";
import { SCM_DORA_DEPLOYMENT_FREQUENCY } from "../../../constants/restUri";

export class scmDoraDeploymentFrequencyService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {} as any) {
    return this.restInstance.post(SCM_DORA_DEPLOYMENT_FREQUENCY, filter, this.options);
  }
}
