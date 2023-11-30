import { restInstance } from "utils/restRequest.js";

export default class BackendService {
  constructor() {
    this.restInstance = restInstance;
    this.options = {};
  }
}
