import BackendService from "./backendService";
import { SMART_TICKET_TEMPLATES } from "constants/restUri";

export class RestSmartTicketTemplateService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = SMART_TICKET_TEMPLATES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = SMART_TICKET_TEMPLATES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = SMART_TICKET_TEMPLATES.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, item) {
    let url = SMART_TICKET_TEMPLATES.concat("/").concat(id.toString());
    let postData = item.json;
    return this.restInstance.put(url, postData, this.options);
  }

  create(item) {
    let postData = item.json;
    return this.restInstance.post(SMART_TICKET_TEMPLATES, postData, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(SMART_TICKET_TEMPLATES, { ...this.options, data: ids });
  }
}
