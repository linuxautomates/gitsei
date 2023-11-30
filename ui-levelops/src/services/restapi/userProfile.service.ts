import { USER_PROFILE } from "constants/restUri";
import { EntityIdentifier } from "types/entityIdentifier";
import BackendService from "./backendService";

export class userProfileService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.update = this.update.bind(this);
  }

  get() {
    return this.restInstance.get(`${USER_PROFILE}/details`, this.options);
  }

  update(id: EntityIdentifier, user: any) {
    let postData = user.json();
    return this.restInstance.put(`${USER_PROFILE}/details`, postData, this.options);
  }
}
