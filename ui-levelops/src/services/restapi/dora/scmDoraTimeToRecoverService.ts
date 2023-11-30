import BackendService from "../backendService";
import { SCM_DORA_TIME_TO_RECOVER } from "../../../constants/restUri";

export class scmDoraTimeToRecoverService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {} as any) {
    return this.restInstance.post(SCM_DORA_TIME_TO_RECOVER, filter, this.options);
  }
}
