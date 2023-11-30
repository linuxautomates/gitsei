export class RestCommTemplate {
  static OPTIONS = Object.freeze(["SLACK", "EMAIL"]);

  static EVENT_OPTIONS = Object.freeze([
    "all",
    "smart_ticket_created",
    "smart_ticket_new_assignee",
    "assessment_submitted"
  ]);

  static DEFAULT_MESSAGE =
    "Please fill out questionnaire $title available at $link being sent to you by $sender\n" +
    "\n" +
    "$text\n" +
    "$info";

  constructor(restData) {
    this._id = undefined;
    this._name = undefined;
    this._message = undefined;
    this._type = undefined;
    this._botname = undefined;
    this._email_subject = undefined;
    this._default = false;
    this._event_type = undefined;
    this._system = undefined;

    if (restData) {
      this._id = restData.id;
      this._name = restData.name;
      this._message = restData.message;
      this._type = restData.type;
      this._botname = restData.bot_name;
      this._email_subject = restData.email_subject;
      this._default = restData.default;
      this._event_type = restData.event_type;
      this._system = restData.system;
    }
    this.json = this.json.bind(this);
  }

  get id() {
    return this._id;
  }
  set id(id) {
    this._id = id;
  }

  get default() {
    return this._default;
  }
  set default(def) {
    this._default = def;
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get message() {
    return this._message;
  }
  set message(mess) {
    this._message = mess;
  }

  get type() {
    return this._type;
  }
  set type(opt) {
    this._type = opt;
  }

  get botname() {
    return this._botname;
  }
  set botname(name) {
    this._botname = name;
  }

  get email_subject() {
    return this._email_subject;
  }

  set email_subject(sub) {
    this._email_subject = sub;
  }

  get event_type() {
    return this._event_type;
  }

  set system(sub) {
    this._system = sub;
  }

  get system() {
    return this._system;
  }

  json() {
    return {
      name: this._name,
      message: this._message,
      type: this._type,
      bot_name: this._botname,
      email_subject: this._email_subject,
      default: this._default,
      event_type: this._event_type
    };
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    valid = valid && data.hasOwnProperty("message");
    valid = valid && data.hasOwnProperty("type");
    return valid;
  }
}
