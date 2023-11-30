import BackendService from "./backendService";
import {
  CICD_FILTER_VALUES,
  CICD_JOB_NAMES,
  CICD_LIST,
  CICD_SCM_JOB_RUNS,
  CICD_USERS,
  JOB_CHANGE_VOLUME_REPORT,
  JOB_COMMITS_LEAD_TIME_CICD_REPORT,
  JOB_COMMITS_LEAD_TIME_REPORT,
  JOB_COUNT_BY_CICD_REPORT,
  JOB_COUNT_REPORT,
  JOB_DURATIONS_BY_CICD_REPORT,
  JOB_DURATIONS_REPORT,
  JOB_STATUSES,
  SCM_USERS,
  PIPELINE_JOB_COUNTS_REPORT,
  PIPELINE_JOB_DURATIONS_REPORT,
  JENKINS_PIPELINE_JOB_RUNS,
  CICD_SCM_JOB_AGG,
  CICD_SCM_JOB_RUN_TESTS,
  CICD_SCM_JOB_RUN_TESTS_FILTER_VALUES,
  CICD_SCM_JOB_RUN_TESTS_REPORT,
  CICD_SCM_JOB_RUNS_TESTS_DURATION_REPORT
} from "constants/restUri";
import { CICD_JOBS, CICD_SCM_JOB_STAGES } from "../../constants/restUri";

export class JobsCountReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JOB_COUNT_REPORT, filter, this.options);
  }
}

export class PipelinesJobsCountReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(PIPELINE_JOB_COUNTS_REPORT, filter, this.options);
  }
}

export class CICDFiltersService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_FILTER_VALUES, filter, this.options);
  }
}

export class JobsCommitsLeadReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JOB_COMMITS_LEAD_TIME_REPORT, filter, this.options);
  }
}

export class JobsChangeVolumeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JOB_CHANGE_VOLUME_REPORT, filter, this.options);
  }
}

export class JobsDurationReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JOB_DURATIONS_REPORT, filter, this.options);
  }
}

export class PipelinesJobsDurationReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(PIPELINE_JOB_DURATIONS_REPORT, filter, this.options);
  }
}

// not processed

export class JobsCountCICDReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JOB_COUNT_BY_CICD_REPORT.concat("/list"), filter, this.options);
  }
}

export class JobsDurationCICDReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JOB_DURATIONS_BY_CICD_REPORT.concat("/list"), filter, this.options);
  }
}

export class JobsCommitsLeadCICDReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JOB_COMMITS_LEAD_TIME_CICD_REPORT.concat("/list"), filter, this.options);
  }
}

export class CICDUsersService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_USERS.concat("/list"), filter, this.options);
  }
}

export class JobStatusesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JOB_STATUSES.concat("/list"), filter, this.options);
  }
}

export class SCMUsersService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_USERS.concat("/list"), filter, this.options);
  }
}

export class CICDJobNamesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_JOB_NAMES.concat("/list"), filter, this.options);
  }
}

export class CICDService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_LIST.concat("/list"), filter, this.options);
  }
}

export class CICDJobsRunsTicketsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_SCM_JOB_RUNS.concat("/list"), filter, this.options);
  }
}

export class CICDJobsAggService extends BackendService {
  constructor() {
    super();
  }

  list = (filter = {}) => this.restInstance.post(CICD_SCM_JOB_AGG.concat("/list"), filter, this.options);
}

export class CICDJobsStagesService extends BackendService {
  constructor() {
    super();
  }

  list = (filter = {}) => this.restInstance.post(CICD_SCM_JOB_STAGES.concat("/list"), filter, this.options);
}

export class PipelineJobRunsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JENKINS_PIPELINE_JOB_RUNS.concat("/list"), filter, this.options);
  }
}

export class PipelineJobRunsLogService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get(id) {
    let options = { ...this.options, responseType: "text/plain" };
    let url = `${JENKINS_PIPELINE_JOB_RUNS}/${id}/log`;
    return this.restInstance.get(url, options);
  }
}

export class PipelineJobRunsStagesLogService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get(id) {
    let options = { ...this.options, responseType: "text" };
    let url = `${JENKINS_PIPELINE_JOB_RUNS}/stages/${id}/log`;
    return this.restInstance.get(url, options);
  }
}

export class JobRunTestsListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_SCM_JOB_RUN_TESTS.concat("/list"), filter, this.options);
  }
}

export class JobRunTestsValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_SCM_JOB_RUN_TESTS_FILTER_VALUES, filter, this.options);
  }
}
export class JobRunTestsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_SCM_JOB_RUN_TESTS_REPORT, filter, this.options);
  }
}

export class JobRunTestsDurationReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_SCM_JOB_RUNS_TESTS_DURATION_REPORT, filter, this.options);
  }
}

export class CICDJobsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${CICD_JOBS}/list`, filters, this.options);
  }
}
