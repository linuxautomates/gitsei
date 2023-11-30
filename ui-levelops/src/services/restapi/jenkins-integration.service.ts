import BackendService from "./backendService";
import { JENKINS_INSTANCES, JENKINS_INSTANCES_ASSIGN } from "../../constants/restUri";

export class JenkinsInstanceListService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.update = this.update.bind(this);
  }

  list(filter = {}) {
    const url = `${JENKINS_INSTANCES}/list`;
    return this.restInstance.post(url, filter, this.options);
  }

  get(id: string) {
    const url = `${JENKINS_INSTANCES}/${id}`;
    return this.restInstance.get(url, this.options);
  }

  update(id: string, data: any) {
    return this.restInstance.post(JENKINS_INSTANCES_ASSIGN, data, this.options);
  }
}
