import BackendService from "./backendService";
import {
  SERVICES__REPORT_AGGREGATES,
  SERVICES__REPORT_AGGREGATES_FILTERS_VALUES,
  SERVICES_REPORT
} from "constants/restUri";

export class ServicesReportWidgetService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  //LIST OF SERVICES
  list(filter = {}) {
    return this.restInstance.post(SERVICES_REPORT.concat("/list"), filter, this.options);
  }

  //GET A PARTICULAR SERVICE DETAILS
  get(id) {
    let url = SERVICES_REPORT.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }
}

export class ServicesReportAggregateService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SERVICES__REPORT_AGGREGATES, filter, this.options);
  }
}

export class ServicesReportAggregateFiltersValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SERVICES__REPORT_AGGREGATES_FILTERS_VALUES, filter, this.options);
  }
}
