import { ISSUE_FILTERS } from "constants/restUri";
import BackendService from "services/backendService";

export class IssueFiltersService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
    this.update = this.update.bind(this);
    this.delete = this.delete.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  get(name: string) {
    if (!name) {
      console.error("name is required");
      return;
    }

    const url = `${ISSUE_FILTERS}/${name}`;
    return this.restInstance.get(url, this.options);
  }

  list(filters = {}) {
    const url = `${ISSUE_FILTERS}/list`;
    return this.restInstance.post(url, filters, this.options);
  }

  create(payload: any) {
    const { name, ...finalPayload } = payload;
    const url = `${ISSUE_FILTERS}/${name}`;
    if (!name) {
      console.error("[] name in payload is required");
      return;
    }
    return this.restInstance.put(url, finalPayload, this.options);
  }

  update(name: string, payload: any) {
    const url = `${ISSUE_FILTERS}/${name}`;
    return this.restInstance.put(url, payload, this.options);
  }

  delete(name: string) {
    const url = `${ISSUE_FILTERS}/${name}`;
    return this.restInstance.delete(url, this.options);
  }

  bulkDelete(names: string) {
    return this.restInstance.delete(ISSUE_FILTERS, { ...this.options, data: names });
  }
}
