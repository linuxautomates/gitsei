import {
  DEV_PRODUCTIVITY_PARENT_PROFILE,
  DEV_PRODUCTIVITY_PARENT_PROFILE_COPY,
  DEV_PRODUCTIVITY_PARENT_PROFILE_DEFAULT,
  DEV_PRODUCTIVITY_PARENT_PROFILE_LIST
} from "constants/restUri";
import BackendService from "services/backendService";

export class RestDevProdParentService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.update = this.update.bind(this);
    this.patch = this.patch.bind(this);
    this.centralProfileUpdate = this.centralProfileUpdate.bind(this);
  }

  list(filters: any = {}) {
    return this.restInstance.post(`${DEV_PRODUCTIVITY_PARENT_PROFILE_LIST}`, filters, this.options);
  }

  get() {
    return this.restInstance.get(`${DEV_PRODUCTIVITY_PARENT_PROFILE_DEFAULT}`);
  }

  update(payload: any) {
    return this.restInstance.put(`${DEV_PRODUCTIVITY_PARENT_PROFILE_COPY}`, payload, this.options);
  }

  patch(ouId: any) {
    return this.restInstance.patch(`${DEV_PRODUCTIVITY_PARENT_PROFILE}/${ouId}/disable-trellis`, this.options);
  }

  centralProfileUpdate(payload: any, id: any) {
    return this.restInstance.put(`${DEV_PRODUCTIVITY_PARENT_PROFILE}/${id}`, payload, this.options);
  }
}
