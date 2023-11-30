import { TRIAGE_GRID_VIEW_FILTERS, TRIAGE_FILTERS } from "constants/restUri";
import BackendService from "services/backendService";

export class TriageGridViewFiltersService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.update = this.update.bind(this);
  }

  get() {
    return this.restInstance.get(TRIAGE_GRID_VIEW_FILTERS, this.options);
  }

  update(id = "0", payload = {}) {
    return this.restInstance.put(TRIAGE_GRID_VIEW_FILTERS, payload, this.options);
  }
}

export class TriageFiltersService extends BackendService {
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

    const url = `${TRIAGE_FILTERS}/${name}`;
    return this.restInstance.get(url, this.options);
  }

  list(filters = {}) {
    const url = `${TRIAGE_FILTERS}/list`;
    return this.restInstance.post(url, filters, this.options);
  }

  create(payload: any) {
    const { name, ...finalPayload } = payload;
    const url = `${TRIAGE_FILTERS}/${name}`;
    if (!name) {
      console.error("[] name in payload is required");
      return;
    }
    return this.restInstance.put(url, finalPayload, this.options);
  }

  update(name: string, payload: any) {
    const url = `${TRIAGE_FILTERS}/${name}`;
    return this.restInstance.put(url, payload, this.options);
  }

  delete(name: string) {
    const url = `${TRIAGE_FILTERS}/${name}`;
    return this.restInstance.delete(url, this.options);
  }

  bulkDelete(names: string) {
    return this.restInstance.delete(TRIAGE_FILTERS, { ...this.options, data: names });
  }
}
