import {
  SONARQUBE,
  SONARQUBE_EFFORT_REPORT,
  SONARQUBE_FILTER_VALUES,
  SONARQUBE_ISSUES_REPORT,
  SONARQUBE_METRICS_REPORT,
  SONARQUBE_METRICS_FILTER_VALUES,
  SONARQUBE_METRICS
} from "constants/restUri";
import BackendService from "services/backendService";

export class SonarQubeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(`${SONARQUBE}/list`, filter, this.options);
  }
}

export class SonarQubeFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SONARQUBE_FILTER_VALUES, filter, this.options);
  }
}

export class SonarQubeIssuesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SONARQUBE_ISSUES_REPORT, filter, this.options);
  }
}

export class SonarQubeEffortReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SONARQUBE_EFFORT_REPORT, filter, this.options);
  }
}

export class SonarQubeMetricsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(`${SONARQUBE_METRICS}/list`, filter, this.options);
  }
}

export class SonarQubeMetricsValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SONARQUBE_METRICS_FILTER_VALUES, filter, this.options);
  }
}

export class SonarQubeMetricsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SONARQUBE_METRICS_REPORT, filter, this.options);
  }
}
