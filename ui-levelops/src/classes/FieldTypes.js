import { validateEmail, validateURL } from "utils/stringUtils";
import {
  DATE,
  DYNAMIC_MULTI_SELECT_TYPE,
  DYNAMIC_SINGLE_SELECT_TYPE,
  FILE_UPLOAD_TYPE,
  MULTI_SELECT_TYPE,
  RADIO_GROUP_TYPE,
  SINGLE_SELECT_TYPE,
  TEXT_AREA_TYPE,
  TEXT_TYPE
} from "constants/fieldTypes";

export class FieldTypes {
  static TYPES = {
    [SINGLE_SELECT_TYPE]: {
      name: "Single Dropdown",
      icon: "down-circle"
    },
    [MULTI_SELECT_TYPE]: {
      name: "Multi Dropdown",
      icon: "down-circle"
    },
    [DYNAMIC_SINGLE_SELECT_TYPE]: {
      name: "Dynamic Single Dropdown",
      icon: "unordered-list"
    },
    [DYNAMIC_MULTI_SELECT_TYPE]: {
      name: "Dynamic Multi Dropdown",
      icon: "unordered-list"
    },
    [RADIO_GROUP_TYPE]: {
      name: "Radio",
      icon: "check-circle"
    },
    [TEXT_TYPE]: {
      name: "Text",
      icon: "font-size"
    },
    [TEXT_AREA_TYPE]: {
      name: "Text Area",
      icon: "font-size"
    },
    [FILE_UPLOAD_TYPE]: {
      name: "File Upload",
      icon: "file-add"
    },
    [DATE]: {
      name: "Date",
      icon: "down-circle"
    }
  };
  // static TYPES = {
  //   "single-dropdown": "down-circle",
  //   "multi-dropdown": "down-circle",
  //   "dynamic-single-dropdown": "unordered-list",
  //   "dynamic-multi-dropdown": "unordered-list",
  //   radio: "check-circle",
  //   text: "font-size",
  //   "text-area": "font-size",
  //   "text-number": "font-size",
  //   "file-upload": "file-add",
  //   date: "calendar",
  //   lql: "lql"
  // };

  static VALIDATIONS = ["not_empty", "email", "url", "number"];

  static DYNAMIC_SELECT = {
    users: { uri: "users", method: "list", searchField: "email" },
    policies: { uri: "policies", method: "list", searchField: "name" },
    repositories: { uri: "repositories", method: "list", searchField: "name" },
    integrations: { uri: "integrations", method: "list", searchField: "name" },
    plugins: { uri: "plugins", method: "list", searchField: "name" },
    tags: { uri: "tags", method: "list", searchField: "name" },
    workitem: { uri: "workitem", method: "list", searchField: "vanity_id" },
    knowledgebase: { uri: "bestpractices", method: "list", searchField: "name" },
    propels: { uri: "propels", method: "list", searchField: "name" },
    projects: { uri: "products", method: "list", searchField: "name" }
  };

  static max_length(value, length = 60) {
    const isValid = !(value !== "" && value !== undefined) || (typeof value === "string" && value.length <= length);
    return FieldTypes.validationStatus(isValid ? null : `This field must not be greater than ${length}`);
  }

  static validationStatus(error = null) {
    return {
      validateStatus: error ? "error" : "success",
      errorMessage: error
    };
  }

  static not_empty(field) {
    const isValid = field !== "" && field !== undefined;
    return FieldTypes.validationStatus(isValid ? null : "This field must not be empty");
  }

  static email(field) {
    return FieldTypes.validationStatus(validateEmail(field) ? null : "This field must be a valid email");
  }

  static url(field) {
    return FieldTypes.validationStatus(validateURL(field) ? null : "This field must be a valid url");
  }

  static number(field) {
    const isValid = !isNaN(field);
    return FieldTypes.validationStatus(isValid ? null : "This field must be a number.");
  }

  constructor(data = null) {
    this._key = "";
    this._type = "text";
    this._required = false;
    this._hidden = false;
    this._options = [];
    this._validation = "";
    this._dynamic_resource_name = undefined;
    this._ticket_template_id = undefined;
    this._search_field = undefined;
    this._id = undefined;
    this._deleted = false;
    this._display_name = undefined;
    if (data !== null) {
      this._id = data.id;
      this._search_field = data.search_field;
      this._key = data.key;
      this._type = data.type;
      this._required = data.required;
      this._hidden = data.hidden;
      this._dynamic_resource_name = data.dynamic_resource_name;
      this._options = data.options;
      this._validation = data.validation;
      this._ticket_template_id = data.ticket_template_id;
      this._deleted = data.deleted !== undefined ? data.deleted : false;
      this._display_name = data.display_name;
    }
  }

  get id() {
    return this._id;
  }

  get deleted() {
    return this._deleted;
  }
  set deleted(deleted) {
    this._deleted = deleted;
  }

  get search_field() {
    return this._search_field;
  }
  set search_field(field) {
    this._search_field = field;
  }

  get display_name() {
    return this._display_name;
  }
  set display_name(name) {
    this._display_name = name;
  }

  get key() {
    return this._key;
  }
  set key(label) {
    this._key = label;
  }

  get type() {
    return this._type;
  }
  set type(type) {
    this._type = type;
  }

  get options() {
    return this._options;
  }
  set options(options) {
    this._options = options;
  }

  get validation() {
    return this._validation;
  }
  set validation(valid) {
    this._validation = valid;
  }

  get required() {
    return this._required;
  }
  set required(req) {
    this._required = req;
  }

  get hidden() {
    return this._hidden;
  }
  set hidden(hide) {
    this._hidden = hide;
  }

  get dynamic_option() {
    if (this._dynamic_resource_name === undefined) {
      return undefined;
    }
    return this._dynamic_resource_name;
  }
  set dynamic_option(option) {
    this._dynamic_resource_name = option;
  }

  get json() {
    return {
      key: this._key,
      type: this._type,
      required: this._required,
      hidden: this._hidden,
      dynamic_resource_name: this._dynamic_resource_name,
      options: this._options,
      validation: this._validation,
      ticket_template_id: this._ticket_template_id,
      search_field: this._search_field,
      display_name: this._display_name,
      deleted: this._deleted,
      id: this._id
    };
  }

  get valid() {
    let valid = true;
    valid = valid && this._type !== undefined;
    if ([SINGLE_SELECT_TYPE, MULTI_SELECT_TYPE, RADIO_GROUP_TYPE].includes(this._type)) {
      valid = valid && this._options.length > 0;
    }
    if ([DYNAMIC_MULTI_SELECT_TYPE, DYNAMIC_SINGLE_SELECT_TYPE].includes(this._type)) {
      valid = valid && this._dynamic_resource_name !== undefined;
    }
    return valid;
  }
}
