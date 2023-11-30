import BackendService from "./backendService";
import { TOOLS } from "constants/restUri";

export class RestToolsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.search = this.search.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    let url = TOOLS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = TOOLS.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = TOOLS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  // delete(id) {
  //     let url = TOOLS.concat("/").concat(id);
  //     return (this.restInstance.delete(url,this.options));
  // }
  //
  // update(id,tool) {
  //     let url = TOOLS.concat("/").concat(id.toString());
  //     let postData = tool.json();
  //     return(this.restInstance.put(url,postData,this.options));
  // }
  //
  // create(tool) {
  //     let postData = tool.json();
  //     return(this.restInstance.post(TOOLS, postData, this.options))
  // }
}
