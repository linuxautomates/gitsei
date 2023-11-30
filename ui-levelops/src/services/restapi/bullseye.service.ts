import {
  BULLSEYE_BUILDS,
  BULLSEYE_BUILDS_COVERAGE_REPORT,
  BULLSEYE_BUILDS_VALUES,
  BULLSEYE_BUILDS_FILES
} from "constants/restUri";
import BackendService from "services/backendService";

export class BullseyeBuildsFilesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = `${BULLSEYE_BUILDS_FILES}/list`;
    return this.restInstance.post(uri, filter, this.options);
  }
}

export class BullseyeBuildsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = `${BULLSEYE_BUILDS}/list`;
    return this.restInstance.post(uri, filter, this.options);
  }
}

export class BullseyeBuildCoverageService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BULLSEYE_BUILDS_COVERAGE_REPORT, filter, this.options);
  }
}

export class BullseyeBuildFilterValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BULLSEYE_BUILDS_VALUES, filter, this.options);
  }
}
