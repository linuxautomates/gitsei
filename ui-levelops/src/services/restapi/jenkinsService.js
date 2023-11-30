import BackendService from "./backendService";
import {
  CICD_JOB_CONFIG_CHANGE,
  JENKINS_FILTER_VALUES,
  JENKINS_JOBS_FILTER_VALUES,
  JENKINS_PIPELINE_FILTER_VALUES,
  JENKINS_PIPELINE_JOBS_RUNS_LIST,
  JENKINS_PIPELINE_JOB_RUNS,
  JENKINS_PIPELINE_JOBS_STAGES_LIST,
  JENKINS_PIPELINE_JOBS_RUNS_TRIAGE_LIST,
  JENKINS_PIPELINE_JOBS_RUNS_TRIAGE,
  CICD_JOB_PARAMS
} from "constants/restUri";

export class JenkinsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_JOB_CONFIG_CHANGE, filter, this.options);
  }
}

export class JenkinsFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JENKINS_FILTER_VALUES, filter, this.options);
  }
}

export class JenkinsJobsFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JENKINS_JOBS_FILTER_VALUES, filter, this.options);
  }
}

export class JenkinsPipelinesJobsFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JENKINS_PIPELINE_FILTER_VALUES, filter, this.options);
  }
}

export class JenkinsPipelinesJobsRunsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JENKINS_PIPELINE_JOBS_RUNS_LIST, filter, this.options);
  }

  get(id) {
    const uri = `${JENKINS_PIPELINE_JOB_RUNS}/${id}`;
    return this.restInstance.get(uri, this.options);
  }
}

export class JenkinsPipelinesJobsTriageService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JENKINS_PIPELINE_JOBS_RUNS_TRIAGE_LIST, filter, this.options);
  }
}

export class JenkinsPipelinesJobStagesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JENKINS_PIPELINE_JOBS_STAGES_LIST, filter, this.options);
  }
}

export class JenkinsJobConfigTicketsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_JOB_CONFIG_CHANGE.concat("/list"), filter, this.options);
  }
}

export class JenkinsJobsExecutionParametersValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(CICD_JOB_PARAMS, filter, this.options);
  }
}