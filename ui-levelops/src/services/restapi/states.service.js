import BackendService from "./backendService";
import { STATES } from "constants/restUri";

export class RestStatesService extends BackendService {
  list = (filter = {}) => {
    let url = STATES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  };

  get = id => {
    let url = STATES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  };

  delete = stateId => {
    let url = STATES.concat("/").concat(stateId);
    return this.restInstance.delete(url, this.options);
  };

  update = (id, state) => {
    let url = STATES.concat("/").concat(id.toString());
    let postData = state.json;
    return this.restInstance.put(url, postData, this.options);
  };

  create = state => {
    let postData = state.json;
    return this.restInstance.post(STATES, postData, this.options);
  };
}
