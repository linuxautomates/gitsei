import BackendService from "../backendService";
import { SCM_DORA_LEAD_TIME } from "../../../constants/restUri";

export class scmDoraLeadTimeService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {} as any) {
    return this.restInstance.post(SCM_DORA_LEAD_TIME, filter, this.options);
  }
}
