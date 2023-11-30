export class RestKBSend {
  constructor() {
    this._best_practices_id = undefined;
    this._sender_email = undefined;
    this._target_email = undefined;
    this._comm_channel = undefined;
    this._work_item_id = undefined;
    this._policy_name = undefined;
    this._artifact = undefined;
    this._integration_url = undefined;
    this._integration_application = undefined;
    this._comm_template_id = undefined;
    this._additional_info = undefined;

    this.json = this.json.bind(this);
  }

  get best_practices_id() {
    return this._best_practices_id;
  }
  set best_practices_id(id) {
    this._best_practices_id = id;
  }

  get work_item_id() {
    return this._work_item_id;
  }
  set work_item_id(id) {
    this._work_item_id = id;
  }

  get target_email() {
    return this._target_email;
  }
  set target_email(id) {
    this._target_email = id;
  }

  get sender_email() {
    return this._sender_email;
  }
  set sender_email(id) {
    this._sender_email = id;
  }

  get policy_name() {
    return this._policy_name;
  }
  set policy_name(name) {
    this._policy_name = name;
  }

  get artifact() {
    return this._artifact;
  }
  set artifact(art) {
    this._artifact = art;
  }

  get integration_url() {
    return this._integration_url;
  }
  set integration_url(url) {
    this._integration_url = url;
  }

  get integration_application() {
    return this._integration_application;
  }
  set integration_application(app) {
    this._integration_application = app;
  }

  get comm_channel() {
    return this._comm_channel;
  }
  set comm_channel(channel) {
    this._comm_channel = channel;
  }

  get comm_template_id() {
    return this._comm_template_id;
  }
  set comm_template_id(id) {
    this._comm_template_id = id;
  }

  get additional_info() {
    return this._additional_info;
  }
  set additional_info(info) {
    this._additional_info = info;
  }

  json() {
    return {
      best_practices_id: this._best_practices_id,
      work_item_id: this._work_item_id,
      target_email: this._target_email,
      sender_email: this._sender_email,
      artifact: this._artifact,
      integration_url: this._integration_url,
      integration_application: this._integration_application,
      comm_channel: this._comm_channel,
      message_template_id: this._comm_template_id,
      additional_info: this._additional_info
    };
  }
}

export class RestKB {
  static TYPE = Object.freeze(["LINK", "TEXT", "FILE"]);

  constructor(restData) {
    this._name = undefined;
    this._value = undefined;
    this._type = "LINK";
    this._id = undefined;
    this._tags = [];
    this._metadata = undefined;
    if (restData) {
      this._name = restData.name;
      this._id = restData.id;
      this._tags = restData.tags === undefined ? [] : restData.tags;
      this._type = restData.type;
      this._value = restData.value;
      this._metadata = restData.metadata;
    }
    this.json = this.json.bind(this);
  }

  get name() {
    return this._name;
  }

  set name(name) {
    this._name = name;
  }

  get tags() {
    return this._tags;
  }

  set tags(tags) {
    this._tags = tags;
  }

  get metadata() {
    return this._metadata;
  }

  set metadata(metadata) {
    this._metadata = metadata;
  }

  get id() {
    return this._id;
  }

  set id(id) {
    this._id = id;
  }

  get type() {
    return this._type;
  }
  set type(t) {
    this._type = t;
  }

  get value() {
    return this._value;
  }
  set value(t) {
    this._value = t;
  }

  json() {
    return {
      name: this._name,
      id: this._id,
      tags: this._tags,
      type: this._type,
      value: this._value,
      metadata: this._metadata
    };
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    valid = valid && data.hasOwnProperty("id");
    valid = valid && data.hasOwnProperty("type");
    return valid;
  }
}
