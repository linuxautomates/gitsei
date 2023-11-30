import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import {
  DEV_PRODUCTIVITY_RELATIVE_SCORE,
  ORG_PIVOT_CREATE,
  ORG_PIVOT_LIST,
  ORG_UNITS,
  ORG_UNITS_DASHBOARDS,
  ORG_UNITS_INTEGRATION_ID,
  ORG_UNITS_VALUES,
  ORG_UNITS_VERSIONS,
  OU
} from "constants/restUri";
import { get, unset } from "lodash";
import { USERROLES } from "routes/helper/constants";
import BackendService from "services/backendService";
import LocalStoreService from "services/localStoreService";

export class OrganizationUnitService extends BackendService {
  constructor() {
    super();
    this.create = this.create.bind(this);
    this.get = this.get.bind(this);
    this.list = this.list.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
    this.update = this.update.bind(this);
    this.parentList = this.parentList.bind(this);
  }

  create(orgUnit: orgUnitJSONType) {
    return this.restInstance.post(ORG_UNITS, orgUnit, this.options);
  }

  update(id: string, orgUnitList: any) {
    return this.restInstance.put(ORG_UNITS, orgUnitList, this.options);
  }

  get(id: string, queryParams: any): any {
    let url = `${ORG_UNITS}/${id}`;
    const version = queryParams?.version || undefined;
    if (version) {
      url = url.concat(`?version=${version}`);
    }
    return this.restInstance.get(url, this.options);
  }

  list(filter = {}, queryParams: any = undefined): any {
    let url = `${ORG_UNITS}/list`;
    const version = queryParams?.version || undefined;
    if (version) {
      url = url.concat(`?version=${version}`);
    }
    return this.restInstance.post(url, filter, this.options);
  }

  bulkDelete(ids: string[]) {
    return this.restInstance.delete(ORG_UNITS, { ...this.options, data: ids });
  }

  parentList(filter = {}, queryParams: any) {
    let url = `${ORG_UNITS}/parentList`;
    const version = queryParams?.version || undefined;
    if (version) {
      url = url.concat(`?version=${version}`);
    }
    return this.restInstance.post(url, filter, this.options);
  }
}

export class OrganizationUnitVersionService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.list = this.list.bind(this);
  }

  get(id: string): any {
    let url = ORG_UNITS_VERSIONS.concat(`?org_id=${id}`);
    return this.restInstance.get(url, this.options);
  }

  list(filter = {}): any {
    return this.restInstance.post(ORG_UNITS_VERSIONS, filter, this.options);
  }
}

export class OrganizationUnitFilterValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}): any {
    return this.restInstance.post(ORG_UNITS_VALUES, filter, this.options);
  }
}

export class OrganizationUnitProductivityScoreService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}): any {
    return this.restInstance.post(DEV_PRODUCTIVITY_RELATIVE_SCORE, filter, this.options);
  }
}

export class OrganizationUnitsForIntegrationService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.list = this.list.bind(this);
  }

  get(id: string): any {
    return this.restInstance.get(`${ORG_UNITS_INTEGRATION_ID}/${id}`, this.options);
  }

  list(filter: Record<string, string>) {
    const URI = `${ORG_UNITS}/${filter?.workspace_id}/integration/list`;
    return this.restInstance.post(URI, filter?.integration_ids, this.options);
  }
}

export class OrganizationUnitPivotListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}): any {
    return this.restInstance.post(ORG_PIVOT_LIST, filter, this.options);
  }
}

export class OrganizationUnitDashboardAssociationService extends BackendService {
  constructor() {
    super();
    this.update = this.update.bind(this);
    this.list = this.list.bind(this);
  }

  update(id: string, data: any): any {
    const url = `${ORG_UNITS_DASHBOARDS}/${id}/dashboards`;
    return this.restInstance.put(url, data, this.options);
  }

  list(filter = {}): any {
    const ouId = get(filter, ["id"]);
    unset(filter, "id");
    const url = `${ORG_UNITS_DASHBOARDS}/${ouId}/dashboards/list`;
    return this.restInstance.post(url, filter, this.options);
  }
}
export class OrganizationUnitPivotCreateUpdateService extends BackendService {
  constructor() {
    super();
    this.create = this.create.bind(this);
    this.update = this.update.bind(this);
  }

  create(filter = {}): any {
    return this.restInstance.post(ORG_PIVOT_CREATE, filter, this.options);
  }

  update(pivot_id: string, data: any) {
    const url = ORG_PIVOT_CREATE.concat(`/${pivot_id}`);
    return this.restInstance.put(url, data, this.options);
  }
}
export class OrganizationUnitDashboardListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}): any {
    const localStorage = new LocalStoreService();
    const ouId = get(filter, ["ou_id"]);
    unset(filter, "ou_id");
    filter = { ...filter, has_rbac_access: true };
    const url: string = `${OU}/${ouId}/dashboards/list`;
    return this.restInstance.post(url, { filter }, this.options);
  }
}
