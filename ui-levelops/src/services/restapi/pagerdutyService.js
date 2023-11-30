import BackendService from "./backendService";
import {
  PAGERDUTY_ACK_TREND,
  PAGERDUTY_AFTER_HOURS,
  PAGERDUTY_RELEASE_INCIDENTS,
  PAGERDUTY_FILTER_VALUES,
  PAGERDUTY_INCIDENT_RATES
} from "constants/restUri";
import { PAGERDUTY_ALERTS_AGGS, PAGERDUTY_INCIDENTS, PAGERDUTY_INCIDENTS_AGGS } from "../../constants/restUri";

export class PagerdutyReleaseIncidents extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(PAGERDUTY_RELEASE_INCIDENTS, filters, this.options);
  }
}

export class PagerdutyAckTrend extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(PAGERDUTY_ACK_TREND, filters, this.options);
  }
}

export class PagerdutyAfterHours extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(PAGERDUTY_AFTER_HOURS, filters, this.options);
  }
}

export class PagerDutyFilterValues extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(PAGERDUTY_FILTER_VALUES, filters, this.options);
  }
}

export class PagerDutyIncidentRatesReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(PAGERDUTY_INCIDENT_RATES, filters, this.options);
  }
}

export class PagerDutyIncidentsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${PAGERDUTY_INCIDENTS}/list`, filters, this.options);
  }
}

export class PagerDutyIncidentsAggsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${PAGERDUTY_INCIDENTS_AGGS}/list`, filters, this.options);
  }
}

export class PagerDutyAlertsAggsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${PAGERDUTY_ALERTS_AGGS}/list`, filters, this.options);
  }
}
