import { AZURE_PIPELINE, AZURE_JOB_DURATION, AZURE_RUNS_REPORT } from "constants/restUri";
import BackendService from "services/backendService";

export class AzurePipelineListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = AZURE_PIPELINE.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }
}

export class AzurePipelineFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = AZURE_PIPELINE.concat("/values");
    return this.restInstance.post(url, filter, this.options);
  }
}

export class AzurePipelineJobDurationReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(AZURE_JOB_DURATION, filter, this.options);
  }
}

export class AzurePipelineRunReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(AZURE_RUNS_REPORT, filter, this.options);
  }
}
