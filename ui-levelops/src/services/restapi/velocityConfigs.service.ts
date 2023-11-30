import BackendService from "./backendService";
import { VELOCITY_CONFIGS } from "constants/restUri";

export class VelocityConfigsService extends BackendService {
  constructor() {
    super();
    this.setDefault = this.setDefault.bind(this);
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
    this.update = this.update.bind(this);
    this.delete = this.delete.bind(this);
    this.baseConfig = this.baseConfig.bind(this);
  }

  list(filter = {}) {
    const url = VELOCITY_CONFIGS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  create(item: any) {
    const postData = item.postData;
    return this.restInstance.post(VELOCITY_CONFIGS, postData, this.options);
  }

  get(id: string) {
    const url = VELOCITY_CONFIGS.concat(`/${id}`);
    return this.restInstance.get(url, this.options);
  }

  update(id: string, item: any) {
    const url = VELOCITY_CONFIGS.concat(`/${id}`);
    const postData = item.postData;
    return this.restInstance.put(url, postData, this.options);
  }

  delete(id: string) {
    const url = VELOCITY_CONFIGS.concat(`/${id}`);
    return this.restInstance.delete(url, this.options);
  }

  setDefault(id: string) {
    const url = VELOCITY_CONFIGS.concat(`/${id}/set-default`);
    return this.restInstance.patch(url, {}, this.options);
  }

  baseConfig() {
    const url = VELOCITY_CONFIGS.concat("/base-config-template");
    return this.restInstance.get(url, this.options);
  }
}
