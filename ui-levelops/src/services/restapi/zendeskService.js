import {
  ZENDESK,
  ZENDESK_AGENT_WAIT_TIME_REPORT,
  ZENDESK_BOUNCE_REPORT,
  ZENDESK_CUSTOM_FILTER_VALUES,
  ZENDESK_FIELDS,
  ZENDESK_FILTER_VALUES,
  ZENDESK_HOPS_REPORT,
  ZENDESK_HYGIENE_REPORT,
  ZENDESK_REOPENS_REPORT,
  ZENDESK_REPLIES_REPORT,
  ZENDESK_REQUESTER_WAIT_TIME_REPORT,
  ZENDESK_RESOLUTION_TIME_REPORT,
  ZENDESK_RESPONSE_TIME_REPORT,
  ZENDESK_TICKETS_REPORT
} from "constants/restUri";

import BackendService from "./backendService";

export class ZendeskTicketsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK.concat("/list"), filter, this.options);
  }
}

export class ZendeskFilterValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_FILTER_VALUES, filter, this.options);
  }
}

export class ZendeskBounceReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_BOUNCE_REPORT, filter, this.options);
  }
}

export class ZendeskHopsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_HOPS_REPORT, filter, this.options);
  }
}

export class ZendeskResponseTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_RESPONSE_TIME_REPORT, filter, this.options);
  }
}

export class ZendeskResolutionTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_RESOLUTION_TIME_REPORT, filter, this.options);
  }
}

export class ZendeskTicketsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_TICKETS_REPORT, filter, this.options);
  }
}

export class ZendeskHygieneReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_HYGIENE_REPORT, filter, this.options);
  }
}

export class ZendeskAgentWaitTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_AGENT_WAIT_TIME_REPORT, filter, this.options);
  }
}

export class ZendeskRequesterWaitTimeReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_REQUESTER_WAIT_TIME_REPORT, filter, this.options);
  }
}

export class ZendeskReopensReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_REOPENS_REPORT, filter, this.options);
  }
}

export class ZendeskRepliesReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_REPLIES_REPORT, filter, this.options);
  }
}

export class ZendeskFieldsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const uri = ZENDESK_FIELDS.concat("/list");
    const updatedFilters = { ...filter, page_size: 1000 };
    return this.restInstance.post(uri, updatedFilters, this.options);
  }
}

export class ZendeskCustomFieldsValuesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ZENDESK_CUSTOM_FILTER_VALUES, filter, this.options);
  }
}
