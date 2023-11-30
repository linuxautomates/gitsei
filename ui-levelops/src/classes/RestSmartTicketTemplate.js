import { FieldTypes } from "./FieldTypes";

export class RestSmartTicketTemplate {
  constructor(data = null) {
    this._id = undefined;
    this._name = "";
    this._description = "";
    this._questionnaire_templates = [];
    this._ticket_fields = [];
    this._questionnaires_select = [];
    this._default_fields = {};
    this._enabled = false;
    this._default = false;
    this._notify_by = { all: ["EMAIL"] };
    this.message_template_ids = [];
    if (data !== null) {
      this._id = data.id;
      this._name = data.name;
      this._description = data.description;
      this._questionnaire_templates = data.questionnaire_templates;
      this._ticket_fields = data.ticket_fields.map(field => new FieldTypes(field));
      this._enabled = data.enabled;
      this._default_fields = data.default_fields;
      this._default = data.default;
      this._notify_by = data.notify_by || [];
      this._message_template_ids = data.message_template_ids || [];
    }
  }

  get id() {
    return this._id;
  }

  get notify_by() {
    return this._notify_by;
  }
  set notify_by(notify) {
    this._notify_by = notify;
  }

  get message_template_ids() {
    return this._message_template_ids;
  }
  set message_template_ids(id) {
    this._message_template_ids = id;
  }

  get ticket_fields() {
    return this._ticket_fields;
  }
  set ticket_fields(fields) {
    this._ticket_fields = fields;
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

  get questionnaire_templates() {
    return this._questionnaire_templates;
  }
  set questionnaire_templates(ques) {
    this._questionnaire_templates = ques;
  }

  get questionnaires_select() {
    return this._questionnaires_select;
  }
  set questionnaires_select(ques_select) {
    this._questionnaires_select = ques_select;
  }

  get enabled() {
    return this._enabled;
  }
  set enabled(dis) {
    this._enabled = dis;
  }

  get default_fields() {
    return this._default_fields;
  }

  set default_fields(values) {
    this._default_fields = values;
  }

  get default() {
    return this._default;
  }
  set default(def) {
    this._default = def;
  }

  get json() {
    return {
      id: this._id,
      name: this._name,
      description: this._description,
      questionnaire_templates: this._questionnaires_select.map(question => ({
        name: question.label,
        questionnaire_template_id: question.key
      })),
      ticket_fields: this._ticket_fields.map(field => field.json),
      default_fields: this._default_fields,
      enabled: this._enabled,
      default: this._default,
      notify_by: this._notify_by,
      message_template_ids: this._message_template_ids
    };
  }

  get valid() {
    let valid = true;
    valid = valid && this._name !== "" && this._name !== undefined;
    this._ticket_fields.forEach(ticket_field => {
      valid = valid && ticket_field.valid;
    });
    return valid;
  }
}
