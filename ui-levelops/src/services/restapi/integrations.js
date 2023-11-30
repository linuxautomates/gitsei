import BackendService from "./backendService";
import { INTEGRATIONS } from "constants/restUri";
import { unset } from "lodash";

export class RestIntegrationsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.bulk = this.list.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = INTEGRATIONS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(integrationId) {
    let url = INTEGRATIONS.concat("/").concat(integrationId);
    return this.restInstance.get(url, this.options);
  }

  delete(integrationId) {
    let url = INTEGRATIONS.concat("/").concat(integrationId);
    return this.restInstance.delete(url, this.options);
  }

  update(id, integration) {
    let url = INTEGRATIONS.concat("/").concat(id.toString());
    let postData = integration.json();

    // Quick fix for now , don't send keys when updating
    unset(postData, ["keys"]);
    unset(postData, ["apikey"]);
    return this.restInstance.put(url, postData, this.options);
  }

  create(integration) {
    let postData = integration.json();
    return this.restInstance.post(INTEGRATIONS, postData, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(INTEGRATIONS, { ...this.options, data: ids });
  }
}
