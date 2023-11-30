import { DEFAULT_FIELDS } from "../constants/fieldTypes";

export const WORKITEM_TYPE_AUTOMATED = "AUTOMATED";
export const WORKITEM_TYPE_MANUAL = "MANUAL";
export const WORKITEM_TICKET_TYPE_SNIPPET = "SNIPPET";

export class RestLinkedItem {
  constructor(restData = null) {
    this._type = undefined;
    this._item = undefined;
    if (restData) {
      this._type = restData?.type;
      this._item = restData?.item;
    }

    this.json = this.json.bind(this);
  }

  get type() {
    return this._type;
  }
  set type(type) {
    this._type = type;
  }

  get item() {
    return this._item;
  }
  set item(item) {
    this._item = item;
  }

  json() {
    return {
      type: this._type,
      item: this._item
    };
  }
}

export class RestWorkItem {
  static STATUS = Object.freeze(["OPEN", "CLOSED", "IN_REVIEW", "NEW"]);

  static TYPE = Object.freeze([WORKITEM_TYPE_AUTOMATED, WORKITEM_TYPE_MANUAL]);

  static RISKS = Object.freeze(["LOW", "MEDIUM", "HIGH", "UNKNOWN"]);

  static DEFAULT_TICKET_TYPE = "STORY";

  constructor(restData = null) {
    this._id = undefined;
    this._name = undefined;
    this._policy_id = undefined;
    this._team_ids = [];
    this._type = undefined;
    this._status = undefined;
    this._state_id = undefined;
    this._due_at = undefined;
    this._linked_item = undefined;
    this._reason = undefined;
    this._integration_id = undefined;
    this._artifact = undefined;
    this._created_at = undefined;
    this._artifact_title = undefined;
    this._cloud_owner = undefined;
    this._product_id = undefined;
    this._assignees = undefined;
    this._vanity_id = undefined;
    this._title = "";
    this._reporter = undefined;
    this._child_ids = undefined;
    this._description = undefined;
    this._ticket_data_values = undefined;
    this._attachments = [];
    this._parent_id = undefined;
    this._ticket_template_id = undefined;
    this._processed = false;
    this._tag_ids = false;
    this._ticket_type = RestWorkItem.DEFAULT_TICKET_TYPE;
    this._notify = true;
    this._default_fields = DEFAULT_FIELDS;
    this._loading = true;
    this._parent_vanity_id = undefined;
    this._cicd_mappings = [];
    this._slack_link = "";
    this._notifications = [];

    if (restData) {
      this._id = restData?.id;
      this._policy_id = restData?.policy_id;
      this._name = restData?.name;
      this._team_ids = restData?.team_ids;
      this._type = restData?.type;
      this._status = restData?.status;
      this._state_id = restData?.state_id;
      this._assignee = restData?.assignee;
      this._due_at = restData?.due_at;
      this._linked_item = new RestLinkedItem(restData.linked_item);
      this._reason = restData?.reason;
      this._integration_id = restData?.integration_id;
      this._artifact = restData?.artifact;
      this._created_at = restData?.created_at;
      this._artifact_title = restData?.artifact_title;
      this._cloud_owner = restData?.cloud_owner;
      this._product_id = restData?.product_id;
      this._assignees = restData?.assignees;
      this._vanity_id = restData?.vanity_id;
      this._title = restData?.title;
      this._reporter = restData?.reporter;
      this._child_ids = restData?.child_ids;
      this._description = restData?.description;
      this._ticket_data_values = restData?.ticket_data_values;
      this._attachments = restData?.attachments;
      this._parent_id = restData?.parent_id;
      this._ticket_template_id = restData?.ticket_template_id;
      this._tag_ids = restData?.tag_ids;
      this._ticket_type = restData?.ticket_type;
      this._notify = restData?.notify;
      this._cicd_mappings = restData?.cicd_mappings || [];
      this._slack_link = restData?.slack_link || "";
      if (restData.notifications) {
        this._notifications = restData?.notifications;
      }
    }
  }

  get slackLink() {
    return this._slack_link;
  }

  get notifications() {
    return this._notifications;
  }

  get cicd_mappings() {
    return this._cicd_mappings;
  }
  set cicd_mappings(mappings) {
    this._cicd_mappings = mappings;
  }

  set parent_vanity_id(id) {
    this._parent_vanity_id = id;
  }
  get parent_vanity_id() {
    return this._parent_vanity_id;
  }

  get loading() {
    return this._loading;
  }
  set loading(loading) {
    this._loading = loading;
  }

  get default_fields() {
    return this._default_fields;
  }
  set default_fields(fields) {
    this._default_fields = fields;
  }

  get notify() {
    return this._notify;
  }
  set notify(notify) {
    this._notify = notify;
  }

  get tag_ids() {
    return this._tag_ids;
  }

  set tag_ids(ids) {
    this._tag_ids = ids;
  }

  get processed() {
    return this._processed;
  }
  set processed(processed) {
    this._processed = processed;
  }

  get ticket_template_id() {
    return this._ticket_template_id;
  }

  set ticket_template_id(ticket_template_id) {
    this._ticket_template_id = ticket_template_id;
  }

  get parent_id() {
    return this._parent_id;
  }

  set parent_id(parentId) {
    this._parent_id = parentId;
  }

  get state_id() {
    return this._state_id;
  }

  set state_id(state_id) {
    this._state_id = state_id;
  }

  get attachments() {
    return this._attachments;
  }
  set attachments(attachment_ids) {
    this._attachments = attachment_ids;
  }

  get description() {
    return this._description;
  }
  set description(description) {
    this._description = description;
  }

  get ticket_data_values() {
    return this._ticket_data_values;
  }

  set ticket_data_values(ticket_data_values) {
    this._ticket_data_values = ticket_data_values;
  }

  get child_ids() {
    return this._child_ids;
  }

  set child_ids(childIds) {
    this._child_ids = childIds;
  }

  get vanity_id() {
    return this._vanity_id;
  }

  get reporter() {
    return this._reporter;
  }

  set reporter(reporter) {
    this._reporter = reporter;
  }

  get title() {
    return this._title;
  }
  set title(title) {
    this._title = title;
  }

  get id() {
    return this._id;
  }
  set id(id) {
    this._id = id;
  }

  get policy_id() {
    return this._policy_id;
  }
  set policy_id(pol) {
    this._policy_id = pol;
  }

  get name() {
    return this._name;
  }
  set name(name) {
    this._name = name;
  }

  get team_ids() {
    return this._team_ids;
  }
  set team_ids(teams) {
    this._team_ids = teams;
  }

  get type() {
    return this._type;
  }
  set type(type) {
    this._type = type;
  }

  get status() {
    return this._status;
  }
  set status(status) {
    this._status = status;
  }

  get assignee() {
    return this._assignee;
  }
  set assignee(assigned) {
    this._assignee = assigned;
  }

  get assignees() {
    return this._assignees;
  }
  set assignees(assigned) {
    this._assignees = assigned;
  }

  get due_at() {
    return this._due_at;
  }
  set due_at(date) {
    this._due_at = date;
  }

  get created_at() {
    return this._created_at;
  }
  set created_at(at) {
    this._created_at = at;
  }

  get linked_item() {
    return this._linked_item;
  }
  set linked_item(jira) {
    this._linked_item = jira;
  }

  get reason() {
    return this._reason;
  }
  set reason(cause) {
    this._reason = cause;
  }

  get integration_id() {
    return this._integration_id;
  }
  set integration_id(id) {
    this._integration_id = id;
  }

  get artifact() {
    return this._artifact;
  }
  set artifact(art) {
    this._artifact = art;
  }

  get artifact_title() {
    return this._artifact_title;
  }
  set artifact_title(art) {
    this._artifact_title = art;
  }

  get cloud_owner() {
    return this._cloud_owner;
  }
  set cloud_owner(owner) {
    this._cloud_owner = owner;
  }

  get product_id() {
    return this._product_id;
  }
  set product_id(product) {
    this._product_id = product;
  }
  get ticket_type() {
    return this._ticket_type;
  }
  set ticket_type(ticket_type) {
    this._ticket_type = ticket_type;
  }

  json() {
    return {
      id: this._id,
      policy_id: this._policy_id,
      name: this._name,
      type: this._type,
      status: this._status,
      state_id: this._state_id,
      assignee: this._assignee,
      assignees: this._assignees,
      due_at: this._due_at,
      reason: this._reason,
      integration_id: this._integration_id,
      created_at: this._created_at,
      artifact: this._artifact,
      artifact_title: this._artifact_title,
      cloud_owner: this._cloud_owner,
      product_id: this._product_id,
      vanity_id: this._vanity_id,
      title: this._title,
      reporter: this._reporter,
      child_ids: this._child_ids,
      description: this._description,
      ticket_data_values: this._ticket_data_values,
      attachments: this._attachments,
      parent_id: this._parent_id,
      ticket_template_id: this._ticket_template_id,
      tag_ids: this._tag_ids,
      ticket_type: this._ticket_type,
      notify: this._notify
    };
  }

  static validate(data) {
    let valid = true;
    //valid = valid && data.hasOwnProperty("policy_id");
    //valid = valid && data.hasOwnProperty("name");
    //valid = valid && data.hasOwnProperty("team_ids") && Array.isArray(data.team_ids);
    // valid = valid && data.hasOwnProperty("type") && RestWorkItem.TYPE.includes(data.type);
    //valid = valid && data.hasOwnProperty("status") && RestWorkItem.STATUS.includes(data.status);
    //valid = valid && data.hasOwnProperty("assignee");
    //valid = valid && data.hasOwnProperty("due_at");
    //valid = valid && data.hasOwnProperty("linked_item");
    // valid = valid && data.hasOwnProperty("reason");
    valid = valid && data.hasOwnProperty("created_at");
    valid = valid && data.hasOwnProperty("vanity_id");
    valid = valid && data.hasOwnProperty("product_id");
    //valid = valid && data.hasOwnProperty("integration_id");
    // valid = valid && data.hasOwnProperty("artifact");

    return valid;
  }
}
