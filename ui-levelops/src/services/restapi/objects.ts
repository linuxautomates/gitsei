import BackendService from "./backendService";
import { OBJECTS } from "constants/restUri";

export class RestObjectsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    const url = OBJECTS.concat("/list_details");
    return this.restInstance.post(url, filter, this.options);
  }

  get(type: string) {
    const url = OBJECTS.concat(`/${type}/fields`);

    const options = {
      ...this.options,
      params: {
        company: "foo"
      }
    };

    return this.restInstance.get(url, options);
  }
}
