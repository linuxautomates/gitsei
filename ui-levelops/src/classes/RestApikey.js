export const APIKEY_TYPE_ADMIN = "ADMIN";
export const APIKEY_TYPE_INGESTION = "INGESTION";

export class RestApikey {
  static ROLES = Object.freeze([APIKEY_TYPE_ADMIN, APIKEY_TYPE_INGESTION]);

  constructor(restData) {
    this._name = "";
    this._description = "";
    this._role = APIKEY_TYPE_ADMIN;
    if (restData) {
      this._name = restData?.name;
      this._description = restData?.description;
      this._role = restData?.role;
    }
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get description() {
    return this._description;
  }
  set description(desc) {
    this._description = desc;
  }

  get role() {
    return this._role;
  }
  set role(role) {
    this._role = role;
  }

  get json() {
    return {
      name: this._name,
      description: this._description,
      role: this._role
    };
  }

  get valid() {
    return this._name !== "" && this._description !== "";
  }
}
