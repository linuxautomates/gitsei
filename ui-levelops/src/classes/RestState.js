export class RestState {
  constructor(restData) {
    this._id = undefined;
    this._name = undefined;
    if (restData) {
      this._id = restData.id;
      this._name = restData.name;
    }
  }

  get id() {
    return this._id;
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get json() {
    return {
      id: this._id,
      name: this._name
    };
  }

  get valid() {
    let valid = true;
    valid = valid && this._name !== undefined && this._name !== "";
    return valid;
  }
}
