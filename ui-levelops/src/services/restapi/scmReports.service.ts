import {
  GITHUB_SCM_REPOS,
  GITHUB_SCM_COMMITTERS,
  GITHUB_SCM_FILE_EXT,
  SCM_REVIEW_COLLABORATION,
  SCM_REPO_NAMES
} from "constants/restUri";
import BackendService from "./backendService";

export class SCMReposReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.bulk = this.bulk.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${GITHUB_SCM_REPOS}/list`, filters, this.options);
  }

  bulk(filters = {}) {
    return this.restInstance.post(`${SCM_REPO_NAMES}/list`, filters, this.options);
  }
}

export class SCMCommittersReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${GITHUB_SCM_COMMITTERS}/list`, filters, this.options);
  }
}

export class SCMFileTypeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(`${GITHUB_SCM_FILE_EXT}/list`, filters, this.options);
  }
}

export class SCMReviewCollaborationReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(SCM_REVIEW_COLLABORATION, filters, this.options);
  }
}
