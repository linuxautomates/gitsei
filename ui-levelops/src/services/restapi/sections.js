import BackendService from "./backendService";
import { SECTIONS } from "constants/restUri";

export class RestSectionsService extends BackendService {
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
    let url = SECTIONS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = SECTIONS.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = SECTIONS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = SECTIONS.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, question) {
    let url = SECTIONS.concat("/").concat(id.toString());
    let postData = question.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(question) {
    let postData = question.json();
    return this.restInstance.post(SECTIONS, postData, this.options);
  }
}
