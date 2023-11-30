import BackendService from "./backendService";
import { FILE_UPLOAD } from "constants/restUri";

export class RestFileService extends BackendService {
  constructor() {
    super();
    this.head = this.head.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
  }

  head(id) {
    let url = FILE_UPLOAD.concat(`/${id}`);
    return this.restInstance.head(url, { responseType: "blob" });
  }

  get(id) {
    let options = { responseType: "blob" };
    let url = FILE_UPLOAD.concat(`/${id}`);
    return this.restInstance.get(url, options);
  }

  delete(id) {
    let url = FILE_UPLOAD.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }
}
