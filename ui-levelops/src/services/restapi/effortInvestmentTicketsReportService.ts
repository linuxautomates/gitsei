import BackendService from "./backendService";
import {
  BA_JIRA_TICKET_COUNT,
  BA_JIRA_STORY_POINT,
  BA_JIRA_ACTIVE_TICKET_COUNT,
  BA_JIRA_ACTIVE_STORY_POINT,
  BA_SCM_JIRA_COMMIT_COUNT,
  BA_JIRA_TICKET_TIME_SPENT
} from "constants/restUri";

export class EffortInvestmentTicketsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_JIRA_TICKET_COUNT, filter, this.options);
  }
}

export class EffortInvestmentStoryPointsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_JIRA_STORY_POINT, filter, this.options);
  }
}

export class EIActiveTicketsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }
  
  list(filter = {}) {
    return this.restInstance.post(BA_JIRA_ACTIVE_TICKET_COUNT, filter, this.options);
  }
}

export class EIActiveStoryPointsReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_JIRA_ACTIVE_STORY_POINT, filter, this.options);
  }
}

export class EISCMCommitsCountService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_SCM_JIRA_COMMIT_COUNT, filter, this.options);
  }
}

export class EITicketTimeSpentService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_JIRA_TICKET_TIME_SPENT, filter, this.options);
  }
}
