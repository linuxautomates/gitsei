import BackendService from "./backendService";
import { CTEMPLATE } from "constants/restUri";

export class RestCtemplatesService extends BackendService {
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
    let url = CTEMPLATE.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = CTEMPLATE.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = CTEMPLATE.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, ctemplate) {
    let url = CTEMPLATE.concat("/").concat(id.toString());
    let postData = ctemplate.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(ctemplate) {
    let postData = ctemplate.json();
    return this.restInstance.post(CTEMPLATE, postData, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(CTEMPLATE, { ...this.options, data: ids });
  }
}
