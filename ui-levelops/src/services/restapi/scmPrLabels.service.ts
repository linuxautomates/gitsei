import BackendService from "./backendService";
import { SCM_PR_LABELS } from "constants/restUri";

export class ScmPrLabelsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_PR_LABELS.concat("/list"), filter, this.options);
  }
}
