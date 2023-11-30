import BackendService from "./backendService";
import { DASHBOARDS } from "constants/restUri";
import { RBAC } from "../../constants/localStorageKeys";

export class DashboardService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
    this.update = this.update.bind(this);
    this.delete = this.delete.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = DASHBOARDS.concat("/list");
    filter = { ...filter, filter: { ...filter.filter, has_rbac_access: true } };
    return this.restInstance.post(url, filter, this.options);
  }

  get(dashboardId) {
    let url = DASHBOARDS.concat("/").concat(dashboardId);
    return this.restInstance.get(url, this.options);
  }

  create(item) {
    return this.restInstance.post(DASHBOARDS, item, this.options);
  }

  update(id, dashboard) {
    const url = DASHBOARDS.concat("/").concat(id.toString());
    return this.restInstance.put(url, dashboard, this.options);
  }

  delete(dashboardId) {
    let url = DASHBOARDS.concat("/").concat(dashboardId);
    return this.restInstance.delete(url, this.options);
  }

  bulkDelete(ids = []) {
    return this.restInstance.delete(DASHBOARDS, { ...this.options, data: ids });
  }
}
