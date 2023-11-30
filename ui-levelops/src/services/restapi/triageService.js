import BackendService from "./backendService";
import { TRIAGE_RULE_RESULT } from "constants/restUri";

export class RestTriageRuleResultService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = TRIAGE_RULE_RESULT.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}
