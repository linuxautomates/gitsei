import { DEFECTS_LIST, DEFECTS_VALUES, DEFECTS_REPORT } from "constants/restUri";
import BackendService from "services/backendService";

export class CoverityDefectsListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(DEFECTS_LIST, filter, this.options);
  }
}

export class CoverityDefectsValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(DEFECTS_VALUES, filter, this.options);
  }
}

export class CoverityDefectsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(DEFECTS_REPORT, filter, this.options);
  }
}
