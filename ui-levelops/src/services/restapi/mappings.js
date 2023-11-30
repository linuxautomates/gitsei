import BackendService from "./backendService";
import { MAPPINGS } from "constants/restUri";

export class RestMappingsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
  }

  list(filter = {}) {
    let url = MAPPINGS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = MAPPINGS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = MAPPINGS.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, mapping) {
    let url = MAPPINGS.concat("/").concat(id.toString());
    let postData = mapping.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(mapping) {
    let postData = mapping.json();
    return this.restInstance.post(MAPPINGS, postData, this.options);
  }
}
