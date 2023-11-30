export class RestPolicyCommunincation {
  static COMM_TYPES = Object.freeze(["levelops_security_chatbot", "direct_message", "email", "jira"]);

  static COMM_ASSIGN_OPTIONS = Object.freeze(["inferred_owner"]);

  constructor(restData = null) {
    this._to = [];
    this._type = [];
    this._jira = undefined;
    if (restData) {
      this._to = restData?.to;
      this._type = restData?.type;
      this._jira = restData.hasOwnProperty("jira") ? restData?.jira : undefined;
    }
    this.json = this.json.bind(this);
  }

  get to() {
    return this._to;
  }

  set to(to) {
    this._to = to;
  }

  get type() {
    return this._type;
  }

  set type(type) {
    this._type = type;
  }

  get jira() {
    return this._jira;
  }

  set jira(jira) {
    // jira will be of the format
    // {integration_id: id, project: project, component: component}
    this._jira = jira;
  }

  json() {
    return {
      to: this._to,
      type: this._type,
      jira: this._jira
    };
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("to") && Array.isArray(data.to);
    valid = valid && data.hasOwnProperty("type") && Array.isArray(data.type);
    valid = valid && data.hasOwnProperty("jira") ? data.jira.constructor === Object : true;
    return valid;
  }
}

export class RestPolicyActions {
  static MANUAL_OPTIONS = Object.freeze(["pen_test", "meeting", "review"]);

  constructor(restData = null) {
    this._communication = {
      template_id: undefined,
      template_type: undefined,
      template_select: {},
      to: [],
      selected: false
    };
    this._assessment = {
      assessment_ids: [],
      assessment_select: [],
      template_id: undefined,
      template_type: undefined,
      template_select: {},
      to: [],
      selected: false
    };
    this._knowledgebase = {
      kb_ids: [],
      kb_select: [],
      template_id: undefined,
      template_type: undefined,
      template_select: {},
      to: [],
      selected: false
    };
    this._workflow = {
      assigning_process: "round_robin",
      assignee_ids: [],
      assignee_select: [],
      selected: false
    };
    this._log = { selected: false };
    if (restData !== null && restData !== undefined) {
      this._knowledgebase = restData.knowledgebase || {
        kb_ids: [],
        kb_select: [],
        template_id: undefined,
        template_type: undefined,
        template_select: {},
        to: [],
        selected: false
      };
      this._assessment = restData.assessment || {
        assessment_ids: [],
        assessment_select: [],
        template_id: undefined,
        template_type: undefined,
        template_select: {},
        to: [],
        selected: false
      };
      this._communication = restData.communication || {
        template_id: undefined,
        template_type: undefined,
        template_select: {},
        to: [],
        selected: false
      };
      this._workflow = restData.workflow || {
        assigning_process: "round_robin",
        assignee_ids: [],
        assignee_select: [],
        selected: false
      };
      this._log = { selected: restData.log } || { selected: false };
    }
    this.json = this.json.bind(this);
    this.validate = this.validate.bind(this);
    this.resetAction = this.resetAction.bind(this);
    this._assessment_json = this._assessment_json.bind(this);
    this._communication_json = this._communication_json.bind(this);
    this._workflow_json = this._workflow_json.bind(this);
    this._kb_json = this._kb_json.bind(this);
    this._log_json = this._log_json.bind(this);
  }

  get knowledgebase() {
    return this._knowledgebase;
  }
  set knowledgebase(kb) {
    this._knowledgebase = kb;
  }

  get assessment() {
    return this._assessment;
  }
  set assessment(ass) {
    this._assessment = ass;
  }

  get communication() {
    return this._communication;
  }
  set communication(comm) {
    this._communication = comm;
  }

  get workflow() {
    return this._workflow;
  }
  set workflow(work) {
    this._workflow = work;
  }

  get log() {
    return this._log;
  }
  set log(log) {
    this._log = log;
  }

  resetAction(action) {
    switch (action) {
      case "knowledgebase":
        this._knowledgebase = {
          kb_ids: [],
          kb_select: [],
          template_id: undefined,
          template_type: undefined,
          template_select: {},
          to: []
        };
        break;
      case "assessment":
        this._assessment = {
          assessment_ids: [],
          assessment_select: [],
          template_id: undefined,
          template_type: undefined,
          template_select: {},
          to: []
        };
        break;
      case "workflow":
        this._workflow = {
          assigning_process: "round_robin",
          assignee_ids: [],
          assignee_select: []
        };
        break;
      case "communication":
        this._communication = {
          template_id: undefined,
          template_type: undefined,
          template_select: {},
          to: []
        };
        break;
      case "log":
        this._log = false;
        break;
      default:
        break;
    }
  }

  _kb_json() {
    if (this._knowledgebase.selected === false) {
      return undefined;
    }
    return {
      kb_ids: this._knowledgebase.kb_ids,
      template_id: this._knowledgebase.template_id,
      to: this._knowledgebase.to
    };
  }

  _assessment_json() {
    if (this._assessment.selected === false) {
      return undefined;
    }
    return {
      assessment_ids: this._assessment.assessment_ids,
      template_id: this._assessment.template_id,
      to: this._assessment.to
    };
  }

  _workflow_json() {
    if (this._workflow.selected === false) {
      return undefined;
    }
    return {
      assigning_process: this._workflow.assigning_process,
      assignee_ids: this._workflow.assignee_ids
    };
  }

  _log_json() {
    if (this._log.selected === false) {
      return undefined;
    } else {
      return true;
    }
  }

  _communication_json() {
    if (this._communication.selected === false) {
      return undefined;
    }
    return {
      template_id: this._communication.template_id,
      to: this._communication.to
    };
  }

  json() {
    return {
      knowledgebase: this._kb_json(),
      assessment: this._assessment_json(),
      communication: this._communication_json(),
      workflow: this._workflow_json(),
      log: this._log_json()
    };
  }

  validate() {
    let valid = true;
    valid = valid && (this._log_json() || this._kb_json() || this._workflow_json() || this._communication_json());
    if (this._knowledgebase.selected !== false) {
      valid = valid && this._knowledgebase.template_id !== undefined;
      valid = valid && this._knowledgebase.to.length > 0;
      valid = valid && this._knowledgebase.kb_ids.length > 0;
    }
    if (this._assessment.selected !== false) {
      valid = valid && this._assessment.template_id !== undefined;
      valid = valid && this._assessment.to.length > 0;
      valid = valid && this._assessment.assessment_ids.length > 0;
    }
    if (this._workflow.selected !== false) {
      valid = valid && this._workflow.assignee_ids.length > 0;
    }
    if (this._communication.selected !== false) {
      valid = valid && this._communication.template_id !== undefined;
      valid = valid && this._communication.to.length > 0;
    }
    return valid;
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("assessment");
    valid = valid && data.hasOwnProperty("workflow");
    valid = valid && data.hasOwnProperty("communication");
    valid = valid && data.hasOwnProperty("knowledgebase");
    valid = valid && data.hasOwnProperty("log");
    return valid;
  }
}

export class RestPolicy {
  static SEVERITY = Object.freeze(["LOW", "MEDIUM", "HIGH"]);

  static ASSIGN_OPTIONS = Object.freeze(["round_robin"]);

  static STATUS = Object.freeze(["ACTIVE", "INACTIVE"]);

  static ACTIONS = Object.freeze(["workflow", "communication", "assessment", "knowledgebase", "log"]);

  constructor(restData = null) {
    this._name = "";
    this._description = "";
    this._severity = "MEDIUM";
    this._assigned = undefined;
    this._status = "ACTIVE";
    this._lqls = [];
    this._actionType = undefined;
    this._actions = new RestPolicyActions();
    this._signature_ids = [];
    this._assignee_ids = [];
    this._assignee_select = [];
    this._assigning_process = "round_robin";
    if (restData) {
      this._name = restData?.name;
      this._description = restData?.description;
      this._severity = restData?.severity;
      this._status = restData?.status;
      this._actions = new RestPolicyActions(restData?.actions);
      this._signature_ids = restData?.signature_ids || [];
      this._lqls = restData?.lqls.map(lql => ({ query: lql, filter: "", valid: true }));
      this._actionType = restData?.action_type;
      this._assignee_ids = restData?.assignee_ids || [];
    }
    this.json = this.json.bind(this);
    this.validate = this.validate.bind(this);
  }

  set name(n) {
    this._name = n;
  }

  get name() {
    return this._name;
  }

  set assignee_ids(ids) {
    this._assignee_ids = ids;
  }

  get assignee_ids() {
    return this._assignee_ids;
  }

  set assignee_select(ids) {
    this._assignee_select = ids;
  }

  get assignee_select() {
    return this._assignee_select;
  }

  set description(desc) {
    this._description = desc;
  }

  get description() {
    return this._description;
  }

  set status(status) {
    this._status = status;
  }

  get status() {
    return this._status;
  }

  set severity(sev) {
    this._severity = sev;
  }

  get severity() {
    return this._severity;
  }

  set lqls(lql) {
    // format
    // {query: q, ...}
    this._lqls = lql;
  }

  get lqls() {
    return this._lqls;
  }

  addLql(lql) {
    this._lqls.push(lql);
  }

  set actionType(type) {
    this._actionType = type;
  }

  get actionType() {
    return this._actionType;
  }

  set actions(act) {
    // this will be a RestPolicyActions object
    this._actions = act;
  }

  get actions() {
    return this._actions;
  }

  get signature_ids() {
    return this._signature_ids;
  }
  set signature_ids(signatures) {
    this._signature_ids = signatures;
  }

  validate() {
    let valid = true;
    valid = valid && this._name !== "";
    valid = valid && this._description !== "";
    valid = valid && this._status !== undefined;
    valid = valid && this._severity !== undefined;
    //valid = valid && this._actions.validate();
    valid = valid && (this._lqls.length > 0 || this._signature_ids.length > 0);
    if (this._lqls.length > 0) {
      this._lqls.forEach(lql => {
        valid = valid && lql.valid;
      });
    }
    return valid;
  }

  json() {
    return {
      name: this._name,
      description: this._description,
      severity: this._severity,
      lqls: this._lqls.filter(lql => lql.query !== undefined).map(lql => lql.query),
      status: this._status,
      assignee_ids: this._assignee_ids,
      assigning_process: this._assigning_process
      //action_type: this._actionType,
      //actions: this._actions.json(),
      //signature_ids: this._signature_ids
    };
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    valid = valid && data.hasOwnProperty("description");
    valid = valid && data.hasOwnProperty("severity");
    valid = valid && data.hasOwnProperty("status");
    valid = valid && data.hasOwnProperty("lqls") && Array.isArray(data.lqls);
    //valid = valid && data.hasOwnProperty("actions");
    //valid = valid && data.hasOwnProperty("signature_ids");
    return valid;
  }
}
