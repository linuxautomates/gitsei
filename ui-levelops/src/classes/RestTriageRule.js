import { validateEmail } from "utils/stringUtils";

export class RestTriageRule {
  constructor(restData) {
    this._id = undefined;
    this._name = undefined;
    this._application = "all";
    this._description = "";
    this._owner = "";
    this._regexes = [];
    this._metadata = {};

    if (restData) {
      this._id = restData.id;
      this._name = restData.name;
      this._application = restData.application;
      this._description = restData.description;
      this._owner = restData.owner;
      this._regexes = restData.regexes || [];
      this._metadata = restData._metadata || {};
    }

    this.addRegex = this.addRegex.bind(this);
    this.removeRegex = this.removeRegex.bind(this);
  }

  get json() {
    return {
      id: this._id,
      name: this._name,
      application: this._application,
      description: this._description,
      owner: this._owner,
      regexes: this._regexes,
      metadata: this._metadata
    };
  }

  get valid() {
    let valid = true;
    valid = valid && this._name !== "" && this._name !== undefined;
    valid = valid && this._owner !== undefined && this._owner !== "" && validateEmail(this._owner);
    // LEV-2105 No validation required for application field.
    // valid =
    //   (valid && !this._application) ||
    //   (!!this._application && this._application === "all") ||
    //   (!!this._application && validateURL(this._application));
    valid = valid && this._regexes.length > 0;
    valid = valid && this._regexes.filter(regex => regex === undefined || regex === "").length === 0;
    return valid;
  }

  addRegex(regex) {
    this._regexes.push(regex);
  }

  removeRegex(id) {
    this._regexes.splice(id, 1);
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get application() {
    return this._application;
  }

  set application(application) {
    this._application = application;
  }

  get description() {
    return this._description;
  }

  set description(description) {
    this._description = description;
  }

  get owner() {
    return this._owner;
  }

  set owner(owner) {
    this._owner = owner;
  }

  get regexes() {
    return this._regexes;
  }
  set regexes(regexes) {
    this._regexes = regexes;
  }

  get id() {
    return this._id;
  }
  set id(id) {
    this._id = id;
  }

  get metadata() {
    return this._metadata;
  }
  set metadata(metadata) {
    this._metadata = metadata;
  }
}
