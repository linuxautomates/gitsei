import BackendService from "./backendService";
import { TAGS } from "constants/restUri";

export class RestTagsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.search = this.search.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.bulk = this.list.bind(this);
  }

  list(filter = {}) {
    let url = TAGS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = TAGS.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = TAGS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = TAGS.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, tag) {
    let url = TAGS.concat("/").concat(id.toString());
    let postData = tag.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(tag) {
    let postData = tag.json();
    return this.restInstance.post(TAGS, postData, this.options);
  }
}
