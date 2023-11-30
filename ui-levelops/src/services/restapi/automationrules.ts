import BackendService from "./backendService";
import { AUTOMATION_RULES } from "constants/restUri";

export class RestAutomationRules extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
    this.update = this.update.bind(this);
    this.delete = this.delete.bind(this);
  }

  list(filter = {}) {
    const url = AUTOMATION_RULES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  create(item: any) {
    const postData = item.json;
    return this.restInstance.post(AUTOMATION_RULES, postData, this.options);
  }

  get(id: string) {
    const url = AUTOMATION_RULES.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  update(id: string, item: any) {
    const url = AUTOMATION_RULES.concat(`/${id}`);
    const postData = item.json;
    return this.restInstance.put(url, postData, this.options);
  }

  delete(id: string) {
    const url = AUTOMATION_RULES.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }
}
