export class RestOrganizations {
  constructor() {
    this._name = undefined;
    this._owner = undefined;
  }

  get name() {
    return this._name;
  }

  set name(nm) {
    this._name = nm;
  }

  get owner() {
    return this._owner;
  }
  set owner(owner) {
    this._owner = owner;
  }

  json() {
    return {
      name: this._name,
      contact: this._owner
    };
  }

  static validate(data) {
    // this will be used to validate the get organization
    let result = true;
    result = result && data.hasOwnProperty("name");
    return result;
  }
}
