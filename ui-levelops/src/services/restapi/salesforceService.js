import BackendService from "./backendService";
import {
  SALESFORCE,
  SALESFORCE_BOUNCE_REPORT,
  SALESFORCE_FILTER_VALUES,
  SALESFORCE_HOPS_REPORT,
  SALESFORCE_HYGIENE_REPORT,
  SALESFORCE_RESOLUTION_TIME_REPORT,
  SALESFORCE_RESPONSE_TIME_REPORT,
  SALESFORCE_TICKETS_REPORT
} from "constants/restUri";

export class SalesForceTicketsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SALESFORCE.concat("/list"), filter, this.options);
  }
}

export class SalesForceFilterValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SALESFORCE_FILTER_VALUES, filter, this.options);
  }
}

export class SalesForceBounceReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SALESFORCE_BOUNCE_REPORT, filter, this.options);
  }
}

export class SalesForceHopsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SALESFORCE_HOPS_REPORT, filter, this.options);
  }
}

export class SalesForceResponseTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SALESFORCE_RESPONSE_TIME_REPORT, filter, this.options);
  }
}

export class SalesForceResolutionTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SALESFORCE_RESOLUTION_TIME_REPORT, filter, this.options);
  }
}

export class SalesForceTicketsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SALESFORCE_TICKETS_REPORT, filter, this.options);
  }
}

export class SalesForceHygieneReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SALESFORCE_HYGIENE_REPORT, filter, this.options);
  }
}
