import BackendService from "./backendService";
import { TRIAGE_RULES } from "constants/restUri";

export class RestTriageRulesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = TRIAGE_RULES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = TRIAGE_RULES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = TRIAGE_RULES.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, rule) {
    let url = TRIAGE_RULES.concat("/").concat(id.toString());
    let postData = rule.json;
    return this.restInstance.put(url, postData, this.options);
  }

  create(rule) {
    let postData = rule.json;
    return this.restInstance.post(TRIAGE_RULES, postData, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(TRIAGE_RULES, { ...this.options, data: ids });
  }
}
