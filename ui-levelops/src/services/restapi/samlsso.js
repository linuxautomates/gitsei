import BackendService from "./backendService";
import { SAML_CONFIG } from "constants/restUri";

export class RestSamlconfigService extends BackendService {
  constructor() {
    super();
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
  }

  get(id = 0) {
    return this.restInstance.get(SAML_CONFIG, this.options);
  }

  delete(id = 0) {
    return this.restInstance.delete(SAML_CONFIG, this.options);
  }

  update(id = 0, samlConfig) {
    let postData = samlConfig.json();
    return this.restInstance.put(SAML_CONFIG, postData, this.options);
  }
}
