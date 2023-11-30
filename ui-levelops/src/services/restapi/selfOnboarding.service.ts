import { SELF_ONBOARDING_REPOS, SELF_ONBOARDING_REPOS_SEARCH } from "constants/restUri";
import BackendService from "services/backendService";

export class SelfOnboardingReposService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(SELF_ONBOARDING_REPOS, filters, this.options);
  }
}

export class SelfOnboardingReposSearchService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filters = {}) {
    return this.restInstance.post(SELF_ONBOARDING_REPOS_SEARCH, filters, this.options);
  }
}
