import BackendService from "./backendService";
import { PLUGINS, FILE_UPLOAD, PLUGIN_RESULTS } from "constants/restUri";

export class RestPluginsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.upload = this.upload.bind(this);
    this.trigger = this.trigger.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = PLUGINS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  upload(id, file) {
    let url = FILE_UPLOAD.concat(`/plugins/${id}`);
    let formData = new FormData();
    formData.append("file", file);
    let options = this.options;
    options.headers = { "Content-Type": "multipart/formdata" };
    return this.restInstance.post(url, formData, options);
  }

  trigger(id, data) {
    const postData = {
      plugin_id: id,
      ...data
    };
    const uri = `${PLUGINS}/${id}/trigger`;
    return this.restInstance.post(uri, postData, this.options);
  }

  get(id) {
    let url = PLUGINS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}

export class RestPluginsCSVService extends BackendService {
  constructor() {
    super();
    this.upload = this.upload.bind(this);
  }

  upload(id, file, data) {
    let url = PLUGIN_RESULTS.concat(`/multipart/pre-process`);
    let formData = new FormData();
    let jsonData = JSON.stringify({
      tool: "csv",
      successful: true,
      version: "1.0",
      ...data
    });
    const blob = new Blob([jsonData], { type: "application/json" });
    formData.append("result", file);
    formData.append("json", blob);
    let options = this.options;
    options.headers = { "Content-Type": "multipart/formdata" };
    return this.restInstance.post(url, formData, options);
  }
}
