import BackendService from "./backendService";
import { QUIZ_AGGS, WORK_ITEM_AGGS, QUIZ_AGGS_PAGINATED } from "constants/restUri";

export class QuizAggsCountsReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = `${QUIZ_AGGS}/count`;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class QuizAggsResponseTimeReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = `${QUIZ_AGGS}/response_time`;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class QuizAggsReponseTimeTableReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = `${QUIZ_AGGS_PAGINATED}/response_time`;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class WorkItemAggsCountsReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let url = `${WORK_ITEM_AGGS}/count`;
    return this.restInstance.post(url, filter, this.options);
  }
}
