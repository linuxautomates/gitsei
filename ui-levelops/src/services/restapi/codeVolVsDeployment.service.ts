import BackendService from "./backendService";
import { CODE_VOL_VS_DEPLOYMENT } from "../../constants/restUri";

export class CodeVolVsDeployemntService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.update = this.update.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CODE_VOL_VS_DEPLOYMENT, filter, this.options);
  }

  get(id: string) {
    const url = `${CODE_VOL_VS_DEPLOYMENT}/${id}`;
    return this.restInstance.get(url, this.options);
  }

  update(id: string, data: any) {
    return this.restInstance.post(CODE_VOL_VS_DEPLOYMENT, data, this.options);
  }
}

export class CodeVolVsDeployemntDrilldownService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(`${CODE_VOL_VS_DEPLOYMENT}/list`, filter, this.options);
  }
}
