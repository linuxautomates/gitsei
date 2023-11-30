import {
  CHANGE_FAILURE,
  DEPLOYMENT_FREQUENCY,
  DORA_COMMITS_DRILL_DOWN,
  DORA_DRILLDOWN_LIST,
  DORA_LEAD_TIME_FOR_CHANGE,
  DORA_LEAD_TIME_FOR_CHANGE_DRILL_DOWN,
  LEAD_TIME_FOR_CHANGE,
  LEAD_TIME_FOR_CHANGE_DRILLDOWN,
  MEAN_TIME_FOR_CHANGE,
  MEAN_TIME_FOR_CHANGE_DRILLDOWN
} from "constants/restUri";
import BackendService from "./backendService";

export class RestDoraChangeFailureReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(CHANGE_FAILURE, filter, this.options);
  }
}

export class RestDoraDeploymentFrequencyReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(DEPLOYMENT_FREQUENCY, filter, this.options);
  }
}

export class RestDoraDrilldownReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(DORA_DRILLDOWN_LIST, filter, this.options);
  }
}

export class RestDoraLeadTimeForChangeReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(DORA_LEAD_TIME_FOR_CHANGE, filter, this.options);
  }
}

export class RestDoraLeadTimeForChangeReportDrillDownService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(DORA_LEAD_TIME_FOR_CHANGE_DRILL_DOWN, filter, this.options);
  }
}

export class RestDoraCommitsDrillDownService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    const URI = `${DORA_COMMITS_DRILL_DOWN}/list`;
    return this.restInstance.post(URI, filter, this.options);
  }
}

export class RestDoraLeadTimeForChangeService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(LEAD_TIME_FOR_CHANGE, filter, this.options);
  }
}

export class RestDoraMeanTimeForChangeService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(MEAN_TIME_FOR_CHANGE, filter, this.options);
  }
}

export class RestDoraLeadTimeForChangeDrilldownService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(LEAD_TIME_FOR_CHANGE_DRILLDOWN, filter, this.options);
  }
}

export class RestDoraMeanTimeToRestoreDrilldownChangeService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter: any = {}) {
    return this.restInstance.post(MEAN_TIME_FOR_CHANGE_DRILLDOWN, filter, this.options);
  }
}