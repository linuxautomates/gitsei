import BackendService from "./backendService";
import { TENANT_STATE } from "constants/restUri";

export class RestTenantStateService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get() {
    return this.restInstance.get(TENANT_STATE, this.options);
  }
}
