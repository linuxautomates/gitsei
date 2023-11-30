import BackendService from "./backendService";
import { SCM_FILES, SCM_FILES_FILTER_VALUES } from "constants/restUri";

export class SCMFilesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_FILES.concat("/list"), filter, this.options);
  }
}

export class SCMFilesReportRootFolderService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_FILES.concat("/report"), filter, this.options);
  }
}

export class SCMFilesFilterValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_FILES_FILTER_VALUES, filter, this.options);
  }
}
