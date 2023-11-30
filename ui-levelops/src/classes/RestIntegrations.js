import { sanitizeObject } from "utils/commonUtils";

export class RestIntegrations {
  constructor() {
    this._id = undefined;
    this._name = undefined;
    this._application = undefined;
    this._description = undefined;
    this._method = undefined;
    this._formData = {};
    this._url = undefined;
    this._support_multiple_api_keys = false;
  }

  get id() {
    return this._id;
  }

  set id(id) {
    this._id = id;
  }

  get name() {
    return this._name;
  }

  set name(nm) {
    this._name = nm;
  }

  get application() {
    return this._application;
  }

  set application(app) {
    this._application = app;
  }

  get description() {
    return this._description;
  }

  set description(desc) {
    this._description = desc;
  }

  get method() {
    return this._method;
  }

  set method(mtd) {
    this._method = mtd;
    if (this._method === "form") {
      if (this._support_multiple_api_keys) {
        this._method = "multiple_api_keys";
      } else {
        this._method = "apikey";
      }
    }
  }

  get formData() {
    return this._formData;
  }

  set formData(fd) {
    this._formData = fd;
  }

  get url() {
    return this._url;
  }

  set url(ul) {
    this._url = ul;
  }

  get supportMultipleApiKeys() {
    return this._support_multiple_api_keys;
  }

  set supportMultipleApiKeys(value) {
    this._support_multiple_api_keys = value;
    if (value) {
      this._method = "multiple_api_keys";
    }
  }

  json() {
    // this is a json constructor for create / update integration
    return sanitizeObject({
      ...this._formData,
      name: this._name,
      application: this._application,
      description: this._description,
      method: this._method,
      url: this._formData.hasOwnProperty("url") ? this._formData.url : this._url
    });
  }

  static validate(data) {
    // this will validate the get integration
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    valid = valid && data.hasOwnProperty("description");
    valid = valid && data.hasOwnProperty("application");
    //valid = valid && data.hasOwnProperty("method");
    valid = valid && data.hasOwnProperty("status");
    return valid;
  }
}
