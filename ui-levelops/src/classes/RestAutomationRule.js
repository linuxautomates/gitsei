import { validateEmail } from "../utils/stringUtils";

export class RestAutomationRule {
  constructor(restData = null, fix = true) {
    this._id = undefined;
    this._name = undefined;
    this._object_type = "";
    this._description = "";
    this._owner = "";
    // TODO: What is use of source field
    this._source = "";
    this._critereas = [];

    // Sometimes I don't want to fix
    // this._critereas. This changes
    // how validation for this._critereas
    // must work too, so I save fix
    // to the instance.
    this._fix = fix;

    if (restData) {
      this._id = restData?.id;
      this._name = restData?.name;
      this._object_type = restData?.object_type;
      this._description = restData?.description;
      this._owner = restData?.owner;
      this._source = restData?.source;
      this._critereas = restData?.critereas || [];
    }

    if (this._fix) {
      // Clean up this._critereas
      // Sometimes, the regexes property (an array) might have
      // empty strings as its elements
      // I remove them here.
      if (Array.isArray(this._critereas)) {
        this._critereas.forEach((criterion, index) => {
          this._critereas[index].regexes = this._critereas[index].regexes.filter(str => !!str);
        });
      }
    }
  }

  get json() {
    return {
      id: this._id,
      name: this._name,
      object_type: this._object_type,
      description: this._description,
      owner: this._owner,
      source: this._source,
      critereas: this._critereas
    };
  }

  get valid() {
    let valid = true;
    valid = valid && this._name !== "" && this._name !== undefined;
    valid = valid && this._owner !== undefined && this._owner !== "" && validateEmail(this._owner);
    valid = valid && this._object_type !== undefined && this._object_type !== "";
    valid = valid && !!this._critereas.length;

    let good = true;

    if (this._fix) {
      good = true;
      for (let criterion of this._critereas) {
        good = good && criterion.field_name !== "" && criterion.field_name !== undefined;
        if (!good) break;
        good = good && !!criterion.regexes.length;
        if (!good) break;
      }
    } else {
      good = true;
      for (let criterion of this._critereas) {
        good = criterion.field_name !== "" && criterion.field_name !== undefined;
        if (!good) break;
      }

      if (good) {
        // Look for one good regex.
        for (let criterion of this._critereas) {
          good = false;

          for (let regex of criterion.regexes) {
            if (!!regex) {
              good = true;
              break;
            }
          }
          // Every criterion must have
          // at least 1 valid regex.
          if (!good) {
            break;
          }
        }
      }
    }

    valid = valid && good;
    return valid;
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get object_type() {
    return this._object_type;
  }

  set object_type(object_type) {
    this._object_type = object_type;
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

  get source() {
    return this._source;
  }

  set source(source) {
    this._source = source;
  }

  get critereas() {
    return this._critereas;
  }

  set critereas(critereas) {
    this._critereas = critereas;
  }

  get id() {
    return this._id;
  }
  set id(id) {
    this._id = id;
  }
}
