import BackendService from "./backendService";
import { PROPEL_NODE_CATEGORIES } from "constants/restUri";

export class RestPropelNodeCategories extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get(id) {
    let url = PROPEL_NODE_CATEGORIES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}
