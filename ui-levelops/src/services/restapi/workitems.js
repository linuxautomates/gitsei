import BackendService from "./backendService";
import { WORK_ITEM, FILE_UPLOAD } from "constants/restUri";

export class RestWorkitemsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.search = this.search.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.patch = this.patch.bind(this);
    this.upload = this.upload.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = WORK_ITEM.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = WORK_ITEM.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = WORK_ITEM.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = WORK_ITEM.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, workitem) {
    const url = WORK_ITEM.concat("/").concat(id.toString());
    const postData = workitem.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(workitem) {
    let postData = workitem.json();
    return this.restInstance.post(WORK_ITEM, postData, this.options);
  }

  patch(id, json) {
    let url = WORK_ITEM.concat("/").concat(id.toString());
    return this.restInstance.patch(url, json, this.options);
  }

  upload(_, file, data) {
    let url = FILE_UPLOAD.concat(`/tickets/${data.workitemId}`);
    let formData = new FormData();
    formData.append("file", file);
    let options = this.options;
    options.headers = { "Content-Type": "multipart/formdata" };
    return this.restInstance.post(url, formData, options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(WORK_ITEM, { ...this.options, data: ids });
  }
}
