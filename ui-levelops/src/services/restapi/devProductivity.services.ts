import {
  DEV_PRODUCTIVITY_FEATURE_DRILLDOWN,
  DEV_PRODUCTIVITY_USER_SNAPSHOT,
  DEV_PRODUCTIVITY_REPORTS_ORGS,
  DEV_PRODUCTIVITY_RELATIVE_SCORE,
  DEV_PRODUCTIVITY_FIXED_INTERVALS_ORGS,
  DEV_PRODUCTIVITY_FIXED_INTERVALS_USERS,
  DEV_PRODUCTIVITY_USERS_PR_ACTIVITY,
  DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT,
  DEVELOPER_RAW_STATS,
  ORG_RAW_STATS,
  DEVELOPER_RAW_STATS_NEW,
  ORG_RAW_STATS_NEW
} from "constants/restUri";
import { timeInterval } from "dashboard/constants/devProductivity.constant";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import BackendService from "./backendService";

export class RestDevProdReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filters: any = {}) {
    const { filter } = filters;
    const { user_id_type, user_id } = filter;
    delete filters.filter.user_id;
    delete filters.filter.user_id_type;
    return this.restInstance.post(
      `${DEV_PRODUCTIVITY_FIXED_INTERVALS_USERS}/${user_id_type}/${user_id}`,
      filters,
      this.options
    );
  }

  get(id: string, params: any = {}) {
    return this.restInstance.get(
      `${DEV_PRODUCTIVITY_USER_SNAPSHOT}?user_id=${params.user_id_list}&user_id_type=${params.user_id_type}`
    );
  }
}

export class RestDevProdDrillDownService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(DEV_PRODUCTIVITY_FEATURE_DRILLDOWN, filter, this.options);
  }
}

export class RestDevProdReportOrgsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(DEV_PRODUCTIVITY_REPORTS_ORGS, filter, this.options);
  }
}

export class RestDevProdReportOrgsUsersService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}, queryparams?: basicMappingType<any>) {
    let url = DEV_PRODUCTIVITY_REPORTS_ORGS;
    if (queryparams?.ou_id) {
      url = `${DEV_PRODUCTIVITY_REPORTS_ORGS}/${queryparams?.ou_id}/users`;
    }
    return this.restInstance.post(url, filter, this.options);
  }
}

export class RestDevProdOrgUnitScoreReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(DEV_PRODUCTIVITY_FIXED_INTERVALS_ORGS.concat("/list"), filter, this.options);
  }
}

export class RestDevProdUserScoreReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(DEV_PRODUCTIVITY_FIXED_INTERVALS_USERS.concat("/list"), filter, this.options);
  }
}
export class RestDevProdRelativeScoreService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(DEV_PRODUCTIVITY_RELATIVE_SCORE, filter, this.options);
  }
}

export class RestDevProdUserPRActivityService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = DEV_PRODUCTIVITY_USERS_PR_ACTIVITY;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class RestDevProdPRActivityReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class RestDevRawStats extends BackendService {
  constructor() {
    super();

    this.list = this.list.bind(this);
    this.newList = this.newList.bind(this);
  }

  list(filter: any = {}) {
    let url = DEVELOPER_RAW_STATS;
    return this.restInstance.post(url, filter, this.options);
  }
  newList(filter: any = {}) {
    let url = DEVELOPER_RAW_STATS_NEW;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class RestOrgRawStats extends BackendService {
  constructor() {
    super();

    this.list = this.list.bind(this);
    this.newList = this.newList.bind(this);
  }

  list(filter: any = {}) {
    let url = ORG_RAW_STATS;
    return this.restInstance.post(url, filter, this.options);
  }
  newList(filter: any = {}) {
    let url = ORG_RAW_STATS_NEW;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class RestDevRawStatsGraph extends BackendService {
  constructor() {
    super();

    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = DEV_PRODUCTIVITY_RELATIVE_SCORE;
    return this.restInstance.post(url, filter, this.options);
  }
}
