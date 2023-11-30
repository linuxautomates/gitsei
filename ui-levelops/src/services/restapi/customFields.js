import BackendService from "./backendService";
import { CUSTOM_FIELDS } from "constants/restUri";

export class RestCustomFieldsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.update = this.update.bind(this);
  }

  list(filter = {}) {
    const url = CUSTOM_FIELDS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  update(id, data) {
    const payload = {
      filter: { work_item_id: id },
      data
    };

    return this.restInstance.put(CUSTOM_FIELDS, payload, this.options);
  }
}
