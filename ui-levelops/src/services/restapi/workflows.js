import BackendService from "./backendService";
import { WORKFLOWS } from "constants/restUri";

export class RestWorkflowsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.search = this.search.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
  }

  list(filter = {}) {
    let url = WORKFLOWS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = WORKFLOWS.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = WORKFLOWS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = WORKFLOWS.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, workflow) {
    let url = WORKFLOWS.concat("/").concat(id.toString());
    let postData = workflow.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(workflow) {
    let postData = workflow.json();
    return this.restInstance.post(WORKFLOWS, postData, this.options);
  }
}
