import BackendService from "./backendService";
import {
  TESTRAILS_TESTS_REPORT,
  TESTRAILS_TESTS_LIST,
  TESTRAILS_TESTS_VALUES,
  TESTRAILS_TESTS_ESTIMATE_REPORT,
  TESTRAILS_TESTS_ESTIMATE_FORECAST_REPORT,
  TESTRAILS_CUSTOM_FIELD,
  TESTRAILS_CUSTOM_FIELD_VALUES
} from "constants/restUri";

export class TestrailsListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(TESTRAILS_TESTS_LIST, filter, this.options);
  }
}

export class TestrailsValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(TESTRAILS_TESTS_VALUES, filter, this.options);
  }
}

export class TestrailsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(TESTRAILS_TESTS_REPORT, filter, this.options);
  }
}

export class TestrailsEstimateReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(TESTRAILS_TESTS_ESTIMATE_REPORT, filter, this.options);
  }
}

export class TestrailsEstimateForecastReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(TESTRAILS_TESTS_ESTIMATE_FORECAST_REPORT, filter, this.options);
  }
}
export class TestrailsFieldsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = TESTRAILS_CUSTOM_FIELD.concat("/list");
    const updatedFilters = { ...filter, page_size: 1000 };
    return this.restInstance.post(uri, updatedFilters, this.options);
  }
}

export class TestrailsCustomFieldsValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(TESTRAILS_CUSTOM_FIELD_VALUES, filter, this.options);
  }
}