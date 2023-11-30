import BackendService from "./backendService";
import { MICROSOFT_THREAT, MICROSOFT_THREAT_LIST, FILTER_VALUES, MICROSOFT_THREAT_REPORT } from "constants/restUri";

const transformFilters = (filter: any = {}) => {
  if (filter?.filter?.priorities) {
    filter.filter.priority = filter.filter.priorities;
    delete filter.filter.priorities;
  }

  if (filter?.filter?.projects) {
    filter.filter.project = filter.filter.projects;
    delete filter.filter.projects;
  }
};

export class MicrosoftIssuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    transformFilters(filter);
    return this.restInstance.post(MICROSOFT_THREAT_LIST, filter, this.options);
  }
}

export class MicrosoftIssueFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    transformFilters(filter);
    return this.restInstance.post(MICROSOFT_THREAT + FILTER_VALUES, filter, this.options);
  }
}

export class MicrosoftThreatModelingIssuesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    transformFilters(filter);
    return this.restInstance.post(MICROSOFT_THREAT_REPORT, filter, this.options);
  }
}
