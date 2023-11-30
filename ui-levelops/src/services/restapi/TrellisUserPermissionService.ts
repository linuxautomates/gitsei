import { TRELLIS_SCOPES } from "constants/restUri";
import BackendService from "services/backendService";

export class TrellisUserPermissionService extends BackendService {
  constructor() {
    super();
    this.update = this.update.bind(this);
  }

  update(scopeActions: string, filter: any) {
    const url = TRELLIS_SCOPES.concat(`${scopeActions}`);
    return this.restInstance.post(url, filter, this.options);
  }
}
