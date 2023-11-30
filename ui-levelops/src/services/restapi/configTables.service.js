import BackendService from "./backendService";
import { CONFIG_TABLES } from "constants/restUri";

export class ConfigTablesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filters = {}) {
    let url = CONFIG_TABLES.concat("/list");
    return this.restInstance.post(url, filters, this.options);
  }

  get(id) {
    let url = CONFIG_TABLES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = CONFIG_TABLES.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, configTable) {
    let url = CONFIG_TABLES.concat("/").concat(id.toString());
    let postData = configTable.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(configTable) {
    let postData = configTable.json();
    return this.restInstance.post(CONFIG_TABLES, postData, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(CONFIG_TABLES, { ...this.options, data: ids });
  }
}
