import BackendService from "./backendService";
import { STAGES } from "constants/restUri";

export class RestStagesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
  }

  list(filter = {}) {
    let url = STAGES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = STAGES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = STAGES.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, stage) {
    let url = STAGES.concat("/").concat(id.toString());
    let postData = stage.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(stage) {
    let postData = stage.json();
    return this.restInstance.post(STAGES, postData, this.options);
  }
}
