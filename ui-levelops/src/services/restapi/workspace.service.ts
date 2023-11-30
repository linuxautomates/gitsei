import { get, unset } from "lodash";
import { ORGANISATION, WORKSPACE } from "constants/restUri";
import BackendService from "services/backendService";

export class WorkspaceService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
    this.update = this.update.bind(this);
    this.delete = this.delete.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
    this.categoriesList = this.categoriesList.bind(this);
  }

  list(filter: Record<string, string>, id = "0") {
    const url: string = `${WORKSPACE}/list`;
    return this.restInstance.post(url, filter, this.options);
  }

  get(id: string): any {
    const url: string = `${WORKSPACE}/${id}`;
    return this.restInstance.get(url, this.options);
  }

  create(filter = {}, id = "0"): any {
    return this.restInstance.post(WORKSPACE, filter, this.options);
  }

  update(data: any, id: string) {
    const url: string = `${WORKSPACE}/${id}`;
    return this.restInstance.put(url, data, this.options);
  }

  delete(id: string) {
    const url: string = `${WORKSPACE}/${id}`;
    return this.restInstance.delete(url, this.options);
  }

  bulkDelete(id: string, ids: string[]) {
    return this.restInstance.delete(WORKSPACE, { ...this.options, data: [...ids] });
  }

  categoriesList(data: any, id: any) {
    const workSpaceID = get(data, ["workspace_id"], "");
    const dashboard_id = get(data, ["dashboard_id"], "");
    unset(data, "workspace_id");
    unset(data, "dashboard_id");
    const url: string = `${WORKSPACE}/${workSpaceID}/categories/dashboard/${dashboard_id}`;
    return this.restInstance.post(url, data, this.options);
  }
}

export class WorkspaceCategoriesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }
  list(filter: Record<string, any>, workspaceId: string, dashboardId: string) {
    const url: string = `${ORGANISATION}/workspaces/${workspaceId}/categories/dashboard/${dashboardId}`;
    return this.restInstance.post(url, filter, this.options);
  }
}
