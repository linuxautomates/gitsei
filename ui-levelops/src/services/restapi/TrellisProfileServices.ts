import { DEV_PRODUCTIVITY_PROFILES } from "constants/restUri";
import BackendService from "services/backendService";

export class TrellisProfileServices extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.update = this.update.bind(this);
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
    this.delete = this.delete.bind(this);
    this.patch = this.patch.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(`${DEV_PRODUCTIVITY_PROFILES}/list`, filter, this.options);
  }

  update(id: string, item: any) {
    const url = DEV_PRODUCTIVITY_PROFILES.concat(`/${id}`);
    return this.restInstance.put(url, item, this.options);
  }

  get(id: string) {
    const url = DEV_PRODUCTIVITY_PROFILES.concat(`/${id}`);
    return this.restInstance.get(url);
  }

  create(item: any) {
    const url = DEV_PRODUCTIVITY_PROFILES;
    return this.restInstance.post(url, item);
  }

  delete(id: string) {
    const url = DEV_PRODUCTIVITY_PROFILES.concat(`/${id}`);
    return this.restInstance.delete(url);
  }

  patch(id: string, data: any) {
    const url = DEV_PRODUCTIVITY_PROFILES.concat(`/${id}/set-ou-mappings`);
    return this.restInstance.patch(url, data);
  }
}
