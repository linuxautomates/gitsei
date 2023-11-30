import { get, unset } from "lodash";
import BackendService from "./backendService";
import { PROPEL, PROPEL_NODES_EVALUATE } from "constants/restUri";

export class RestPropels extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.bulk = this.bulk.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = PROPEL.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = PROPEL.concat("/permanent-id/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = PROPEL.concat("/permanent-id/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, item) {
    let url = PROPEL.concat("/permanent-id/").concat(id.toString());
    let postData = item.post_data;
    return this.restInstance.put(url, postData, this.options);
  }

  create(item) {
    let postData = item.post_data;
    return this.restInstance.post(PROPEL, postData, this.options);
  }

  bulk(filter = {}) {
    let url = PROPEL.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(PROPEL, { ...this.options, data: ids });
  }
}
export class PropelRunsLogsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    const propelId = get(filter, ["filter", "propel_id"], "");
    const runId = get(filter, ["filter", "run_id"], "");
    unset(filter, ["filter", "propel_id"]);
    unset(filter, ["filter", "run_id"]);
    let url = `${PROPEL}/${propelId}/runs/${runId}/nodes/list`;
    return this.restInstance.post(url, filter, this.options);
  }
}

export class PropelNodesEvaluateService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(PROPEL_NODES_EVALUATE, filter, this.options);
  }
}
