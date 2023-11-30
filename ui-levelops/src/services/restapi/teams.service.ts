import { TEAMS } from "constants/restUri";
import BackendService from "services/backendService";

export class TeamsListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const url = `${TEAMS}/list`;
    return this.restInstance.post(url, filter, this.options);
  }
}
