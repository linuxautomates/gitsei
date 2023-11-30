import BackendService from "./backendService";
import {
  ISSUE_MANAGEMENT_STAGE_TIME_REPORT,
  ISSUE_MANAGEMENT_TICKET_REPORT,
  ISSUE_MANAGEMENT_AGE_REPORT,
  ISSUE_MANAGEMENT_HYGIENE_REPORT,
  ISSUE_MANAGEMENT_STORY_POINT_REPORT,
  ISSUE_MANAGEMENT_RESPONSE_TIME_REPORT,
  ISSUE_MANAGEMENT_RESOLUTION_TIME_REPORT,
  ISSUE_MANAGEMENT_SPRINT_REPORT,
  ISSUE_MANAGEMENT_WORKITEM_ATTRIBUTES_VALUES,
  BA_AZURE_TICKET_COUNT,
  BA_AZURE_STORY_POINT,
  BA_AZURE_COMMIT_COUNT,
  BA_AZURE_TICKET_TIME_SPENT,
  BA_AZURE_ACTIVE_TICKET_COUNT,
  BA_AZURE_ACTIVE_STORY_POINT,
  BA_AZURE_TEAM_ALLOCATION,
  ISSUE_MANAGEMENT_EFFORT_REPORT
} from "../../constants/restUri";

export class IssueManagementTicketReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_TICKET_REPORT, filter, this.options);
  }
}

export class IssueManagementStageTimeReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_STAGE_TIME_REPORT, filter, this.options);
  }
}

export class IssueManagementAgeReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_AGE_REPORT, filter, this.options);
  }
}

export class IssueManagementHygieneReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_HYGIENE_REPORT, filter, this.options);
  }
}
export class IssueManagementStoryPointReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_STORY_POINT_REPORT, filter, this.options);
  }
}
export class IssueManagementEffortReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_EFFORT_REPORT, filter, this.options);
  }
}

export class IssueManagementResponseTimeReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_RESPONSE_TIME_REPORT, filter, this.options);
  }
}

export class IssueManagementResolutionTimeReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_RESOLUTION_TIME_REPORT, filter, this.options);
  }
}

export class IssueManagementSprintReport extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_SPRINT_REPORT, filter, this.options);
  }
}

export class IssueManagementAttributesValues extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ISSUE_MANAGEMENT_WORKITEM_ATTRIBUTES_VALUES, filter, this.options);
  }
}

/** Effort Investment Completed Work Azure Service */
export class IssueManagementEITicketCountFTE extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_AZURE_TICKET_COUNT, filter, this.options);
  }
}

export class IssueManagementEIStoryPointFTE extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_AZURE_STORY_POINT, filter, this.options);
  }
}

export class IssueManagementEICommitCountFTE extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_AZURE_COMMIT_COUNT, filter, this.options);
  }
}

export class IssueManagementEITicketTimeSpentFTE extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_AZURE_TICKET_TIME_SPENT, filter, this.options);
  }
}

export class IssueManagementEIActiveTicketCountFTE extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_AZURE_ACTIVE_TICKET_COUNT, filter, this.options);
  }
}

export class IssueManagementEIActiveStoryPointFTE extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_AZURE_ACTIVE_STORY_POINT, filter, this.options);
  }
}

export class IssueManagementEITeamAllocation extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(BA_AZURE_TEAM_ALLOCATION, filter, this.options);
  }
}
