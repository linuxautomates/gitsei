import BackendService from "./backendService";
import {
  ORG_USERS_VERSIONS,
  ORG_USERS,
  ORG_USERS_IMPORT,
  ORG_USERS_SCHEMA,
  ORG_USERS_FILTER,
  CONTRIBUTORS_ROLES
} from "../../constants/restUri";

export class RestOrgUsersService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.list = this.list.bind(this);
    this.create = this.create.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
    this.update = this.update.bind(this);
  }

  list(filter = {}, queryParams: any = {}) {
    const version = queryParams?.version || undefined;
    let url = ORG_USERS.concat("/list");
    if (version) {
      url = ORG_USERS.concat(`/list?version=${version}`);
    }
    return this.restInstance.post(url, filter, this.options);
  }

  create(item: any) {
    return this.restInstance.post(ORG_USERS, item, this.options);
  }

  bulkDelete(ids: string[]) {
    return this.restInstance.delete(ORG_USERS, { ...this.options, data: ids });
  }

  get(id: string) {
    let url = ORG_USERS.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  update(id: any, item: any) {
    return this.restInstance.put(ORG_USERS, item, this.options);
  }
}

export class RestOrgUsersVersionService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.list = this.list.bind(this);
    this.create = this.create.bind(this);
  }

  list(filter = {}) {
    let url = ORG_USERS_VERSIONS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  create(item: any) {
    return this.restInstance.post(ORG_USERS_VERSIONS, item, this.options);
  }

  get(id: string) {
    return this.restInstance.get(`${ORG_USERS_VERSIONS}?page_size=200`, this.options);
  }
}

export class RestOrgUsersImportService extends BackendService {
  constructor() {
    super();
    this.create = this.create.bind(this);
  }

  create(item: any) {
    const formData = new FormData();
    formData.append("file", item.file);
    formData.append("type", item.type);
    formData.append("import_mode", item.import_mode);
    let options: any = this.options;
    options.headers = { "Content-Type": "multipart/formdata" };
    return this.restInstance.post(ORG_USERS_IMPORT, formData, this.options);
  }
}

export class RestOrgUsersSchemaService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
  }

  get(id: string, queryParams: any) {
    let url = ORG_USERS_SCHEMA;
    const version = queryParams?.version || undefined;
    if (version) {
      url = url.concat(`?version=${version}`);
    }
    return this.restInstance.get(url, this.options);
  }

  create(item: any) {
    return this.restInstance.post(ORG_USERS_SCHEMA, item, this.options);
  }
}

export class RestOrgUsersFilterService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(ORG_USERS_FILTER, filter, this.options);
  }
}

export class RestOrgUsersContributorsRolesService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
  }

  get(filter = {}) {
    return this.restInstance.get(CONTRIBUTORS_ROLES, filter);
  }
}