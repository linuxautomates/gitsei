import BackendService from "./backendService";
import { JIRA_LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT, JIRA_RELEASE_TABLE_REPORT, JIRA_RELEASE_TABLE_REPORT_DRILLDOWN, LEAD_TIME_REPORT, LEAD_TIME_VALUES } from "constants/restUri";

export class LeadTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(LEAD_TIME_REPORT, filter, this.options);
  }
}

export class LeadTimeValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(LEAD_TIME_VALUES, filter, this.options);
  }
}

export class LeadTimeByTimeSpentInStagesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT, filter, this.options);
  }
}

export class jiraReleaseTableReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_RELEASE_TABLE_REPORT, filter, this.options);
  }
}
export class jiraReleaseTableReportDrilldownService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_RELEASE_TABLE_REPORT_DRILLDOWN, filter, this.options);
  }
}