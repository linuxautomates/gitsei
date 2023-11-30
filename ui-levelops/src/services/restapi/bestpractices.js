import BackendService from "./backendService";
import { BPS, FILE_UPLOAD } from "constants/restUri";

export class RestBestpracticesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.search = this.search.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.send = this.send.bind(this);
    this.create = this.create.bind(this);
    this.upload = this.upload.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = BPS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = BPS.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(bpsId) {
    let url = BPS.concat("/").concat(bpsId);
    return this.restInstance.get(url, this.options);
  }

  delete(bpsId) {
    let url = BPS.concat("/").concat(bpsId);
    return this.restInstance.delete(url, this.options);
  }

  update(id, bps) {
    let url = BPS.concat("/").concat(id.toString());
    let postData = bps.json();
    return this.restInstance.put(url, postData, this.options);
  }

  send(bps) {
    let url = BPS.concat(`/send`);
    let postData = bps.json();
    return this.restInstance.post(url, postData, this.options);
  }

  create(bps) {
    let postData = bps.json();
    return this.restInstance.post(BPS, postData, this.options);
  }

  upload(id, file) {
    let url = FILE_UPLOAD.concat(`/kb/${id}`);
    let formData = new FormData();
    formData.append("file", file);
    let options = this.options;
    options.headers = { "Content-Type": "multipart/formdata" };
    console.log("file upload");
    return this.restInstance.post(url, formData, options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(BPS, { ...this.options, data: ids });
  }
}
