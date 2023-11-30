import { INTEGRATIONS, JIRA_V2_GENERATE_TOKEN, JIRA_V2_VERIFY_CONNECTION } from "constants/restUri";
import BackendService from "services/backendService";
import { jiraIntegrationPayload } from "./JiraV2.types";

export class JiraCloudGenerateTokenService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get(filter = {}) {
    let url = JIRA_V2_GENERATE_TOKEN;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class JiraCloudVerifyConnectionService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get(otp: string) {
    let url = `${JIRA_V2_VERIFY_CONNECTION}/?otp=${otp}`;
    return this.restInstance.post(url, this.options);
  }
}

export class JiraCloudCreateIntegration extends BackendService {
  constructor() {
    super();
    this.create = this.create.bind(this);
  }

  create(integration: jiraIntegrationPayload) {
    let postData = integration;
    return this.restInstance.post(INTEGRATIONS, postData, this.options);
  }
}
