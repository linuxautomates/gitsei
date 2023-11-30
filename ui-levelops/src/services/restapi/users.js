import BackendService from "./backendService";
import { USERS, USER_PROFILE } from "constants/restUri";

export class RestUsersService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.me = this.me.bind(this);
  }

  list(filter = {}) {
    let url = USERS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(userId) {
    let url = USERS.concat("/").concat(userId);
    return this.restInstance.get(url, this.options);
  }

  delete(userId) {
    let url = USERS.concat("/").concat(userId);
    return this.restInstance.delete(url, this.options);
  }

  update(id, user) {
    let url = USERS.concat("/").concat(id.toString());
    let postData = user.json();
    return this.restInstance.put(url, postData, this.options);
  }

  bulk = (data) => {
    let url = USERS.concat("/multi_update");
    return this.restInstance.post(url, data, this.options);
  }

  create(user) {
    let postData = user.json();
    return this.restInstance.post(USERS, postData, this.options);
  }

  me() {
    return this.restInstance.get(USER_PROFILE, this.options);
  }
}
