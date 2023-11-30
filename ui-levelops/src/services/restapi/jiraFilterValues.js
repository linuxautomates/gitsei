import BackendService from "./backendService";
import { JIRA_CUSTOM_CONFIG, JIRA_CUSTOM_FILTER_VALUES, JIRA_FILTER_VALUES } from "constants/restUri";
import { JIRA_FIELDS, JIRAPROJECTS } from "../../constants/restUri";

export class JiraFilterValueService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_FILTER_VALUES, filter, this.options);
  }
}

export class JiraCustomFieldsValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRA_CUSTOM_FILTER_VALUES, filter, this.options);
  }
}

export class JiraCustomFieldsConfigService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.create = this.create.bind(this);
  }

  list(filter = {}) {
    const uri = JIRA_CUSTOM_CONFIG.concat("/list");
    return this.restInstance.post(uri, filter, this.options);
  }

  create(data) {
    return this.restInstance.post(JIRA_CUSTOM_CONFIG, data, this.options);
  }
}

export class JiraFieldsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = JIRA_FIELDS.concat("/list");
    const updatedFilters = { ...filter, page_size: 1000 };
    return this.restInstance.post(uri, updatedFilters, this.options);
  }
}

export class JiraProjectsFilterValue extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(JIRAPROJECTS.concat("/values"), filter, this.options);
  }
}
