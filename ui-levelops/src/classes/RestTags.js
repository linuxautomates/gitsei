export class RestTags {
  constructor(restData = null) {
    this._name = undefined;
    this._parent_id = undefined;
    this._id = undefined;
    if (restData) {
      this._id = restData?.id;
      this._name = restData?.name;
      this._parent_id = restData?.parent_id;
    }
  }

  get name() {
    return this._name;
  }

  set name(name) {
    this._name = name;
  }

  get parent_id() {
    return this._parent_id;
  }

  set parent_id(parent) {
    this._parent_id = parent;
  }

  json() {
    return {
      id: this._id,
      name: this._name,
      parent_id: this._parent_id
    };
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    valid = valid && data.hasOwnProperty("parent_id");
    valid = valid && data.hasOwnProperty("id");
    return valid;
  }

  static getNewAndExistingTags(tags) {
    const newTags = [];
    const existingTags = [];

    if (tags && tags.length > 0) {
      for (const tag of tags) {
        if (tag.key.startsWith("create:")) {
          newTags.push(tag);
        } else {
          existingTags.push(tag);
        }
      }
    }

    return {
      newTags,
      existingTags
    };
  }
}
