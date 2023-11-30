import BackendService from "./backendService";
import { SCM_JIRA_FILES } from "constants/restUri";

export class SCMJiraFilesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_JIRA_FILES.concat("/list"), filter, this.options);
  }
}

export class SCMJiraFilesRootFolderReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(SCM_JIRA_FILES.concat("/report"), filter, this.options);
  }
}
