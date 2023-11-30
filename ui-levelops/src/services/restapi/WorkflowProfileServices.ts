import { VELOCITY_CONFIGS } from "constants/restUri";
import BackendService from "services/backendService";

export class WorkflowProfileServices extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.update = this.update.bind(this);
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
    this.delete = this.delete.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(`${VELOCITY_CONFIGS}/list`, filter, this.options);
  }

  update(id: string, item: any) {
    const url = VELOCITY_CONFIGS.concat(`/${id}`);
    return this.restInstance.put(url, item, this.options);
  }

  get(id: string) {
    const url = VELOCITY_CONFIGS.concat(`/${id}`);
    return this.restInstance.get(url);
  }

  create(item: any) {
    const url = VELOCITY_CONFIGS;
    return this.restInstance.post(url, item);
  }

  delete(id: string) {
    const url = VELOCITY_CONFIGS.concat(`/${id}`);
    return this.restInstance.delete(url);
  }
}

export class WorkflowProfileServicesByOu extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }
  get(id: string) {
    const url = VELOCITY_CONFIGS.concat(`/ou/${id}`);
    return this.restInstance.get(url);
  }
}
