import BackendService from "./backendService";
import { JIRA_ISSUES } from "../../constants/restUri";

export class JiraSprintDistributionReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {} as any) {
    const url = `${JIRA_ISSUES}/sprint_distribution_report`;
    return this.restInstance.post(url, filter, this.options);
  }
}
