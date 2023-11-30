import BackendService from "./backendService";
import { WORKITEM_PRIORITIES } from "../../constants/restUri";

export class WorkItemPrioritiesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.update = this.update.bind(this);
  }

  list(filter = {}) {
    let url = WORKITEM_PRIORITIES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  update(id: string, bulkPriorities: unknown) {
    let url = WORKITEM_PRIORITIES.concat("/bulk");
    return this.restInstance.put(url, bulkPriorities, this.options);
  }
}
