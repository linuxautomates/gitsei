export class RestQuestion {
  static TYPES = Object.freeze(["multi-select", "single-select", "text", "file upload", "boolean", "checklist"]);

  static ICON_MAP = Object.freeze({
    "multi-select": { icon: "down-circle", name: "Choice - Check Boxes" },
    "single-select": { icon: "down-circle", name: "Choice - Radio Buttons" },
    text: { icon: "font-size", name: "Multi-Line Textbox" },
    "file upload": { icon: "upload", name: "File Upload" },
    boolean: { icon: "check-circle", name: "Yes/No" },
    checklist: { icon: "ordered-list", name: "Checklist" }
  });

  static VERIFICATION_MODES = Object.freeze(["manual", "auto"]);

  static SCORES = Object.freeze([0, 1, 3, 5]);

  static DEFAULT_BOOLEAN_OPTIONS = Object.freeze([
    { value: "yes", score: 1 },
    { value: "no", score: 5 }
  ]);

  constructor(restData = null) {
    this._id = undefined;
    this._name = "";
    this._type = "default";
    this._options = [];
    this._custom = undefined;
    this._verifiable = undefined;
    this._verification_mode = undefined;
    this._verification_assets = undefined;
    this._training = undefined;
    this._user_email = undefined;
    this._number = undefined;
    this._required = false;
    this._fill_in = false;

    if (restData) {
      if (restData.hasOwnProperty("id")) {
        this._id = restData?.id;
      }
      this._name = restData?.name;
      this._type = restData?.type;
      if (restData.hasOwnProperty("options")) {
        this._options = restData?.options;
      } else {
        this._options = [{ score: 0, value: "" }];
      }
      this._user_email = restData?.user_email;
      this._custom = restData?.custom;
      this._verifiable = restData?.verifiable;
      this._verification_mode = restData?.verification_mode;
      this._verification_assets = restData?.verification_assets;
      this._training = restData?.training;
      this._number = restData?.number;
      this._required = restData?.required || false;
      if (restData.hasOwnProperty("fill_in")) {
        this._fill_in = restData?.fill_in;
      }
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

  get custom() {
    return this._custom;
  }

  set custom(custom) {
    this._custom = custom;
  }

  get verifiable() {
    return this._verifiable;
  }

  set verifiable(ver) {
    this._verifiable = ver;
  }

  get verification_mode() {
    return this._verification_mode;
  }

  set verification_mode(mode) {
    this._verification_mode = mode;
  }

  get verification_assets() {
    return this._verification_assets;
  }

  set verification_assets(assets) {
    this._verification_assets = assets;
  }

  get training() {
    return this._training;
  }

  set training(training) {
    this._training = training;
  }

  get number() {
    return this._number;
  }
  set number(order) {
    this._number = order;
  }

  get required() {
    return this._required;
  }
  set required(req) {
    this._required = req;
  }

  get fill_in() {
    return this._fill_in;
  }

  set fill_in(val) {
    this._fill_in = val;
  }

  json() {
    return {
      id: this._id,
      name: this._name,
      type: this._type,
      options: this._options,
      custom: this._custom,
      verifiable: this._verifiable,
      verification_mode: this._verification_mode,
      verification_assets: this._verification_assets,
      training: this._training,
      number: this._number,
      required: this._required,
      fill_in: this._fill_in
    };
  }

  valid() {
    let valid = true;
    valid = valid && this._name !== "";
    valid = valid && this._options.length > 0;
    if (this._type !== "checklist") {
      this._options.forEach(option => {
        if (this._type === "single-select" || this._type === "multi-select") {
          if (option.value === "" || option.value === undefined) {
            valid = false;
          }
        }

        if (option.score === undefined) {
          valid = false;
        }
      });
    } else {
      this._options.forEach(option => {
        if (option.score === undefined) {
          valid = false;
        }
      });
    }
    return valid;
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    valid =
      valid && data.hasOwnProperty("type") && this.TYPES.includes(data.type) && data.type.includes("select")
        ? data.hasOwnProperty("options") && Array.isArray(data.options)
        : true;
    //valid = valid && data.hasOwnProperty("custom") && typeof data.custom === 'boolean';
    //valid = valid && data.hasOwnProperty("verifiable") && typeof data.verifiable === 'boolean';
    //valid = valid && data.hasOwnProperty("verification_mode") &&
    //    this.VERIFICATION_MODES.includes(data.verification_mode);
    //valid = valid && data.hasOwnProperty("verification_assets") && Array.isArray(data.verification_assets);
    //valid = valid && data.hasOwnProperty("training") && Array.isArray(data.training);
    return valid;
  }
}
export class RestSection {
  static TYPES = Object.freeze(["default", "checklist"]);

  constructor(restData = null) {
    this._name = "";
    this._description = "";
    this._id = undefined;
    this._questions = [];
    this._type = "default";
    this._risk_enabled = false;

    if (restData) {
      if (restData?.hasOwnProperty("id")) {
        this._id = restData?.id;
      }
      this._name = restData?.name;
      this._questions = restData?.questions.map(question => new RestQuestion(question));
      this._description = restData?.description || "";
      if (restData?.hasOwnProperty("type")) {
        this._type = restData?.type;
      }
      this._risk_enabled = restData?.risk_enabled || false;
    }
  }

  get id() {
    return this._id;
  }

  set id(id) {
    this._id = id;
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

  set questions(assertions) {
    this._questions = assertions;
  }

  get questions() {
    return this._questions;
  }

  get type() {
    return this._type;
  }
  set type(type) {
    this._type = type;
  }

  get risk_enabled() {
    return this._risk_enabled;
  }
  set risk_enabled(risk) {
    this._risk_enabled = risk;
  }

  json() {
    let jsonObj = {
      name: this._name,
      description: this._description,
      type: this._type,
      questions: this._questions.map(assertion => assertion.json()),
      risk_enabled: this._risk_enabled
    };
    if (!this._risk_enabled) {
      jsonObj.questions.forEach(question => {
        question.options.forEach(option => {
          option.score = 0;
        });
      });
    }
    return jsonObj;
    // return(
    //     {
    //         name: this._name,
    //         description: this._description,
    //         type: this._type,
    //         questions: this._questions.map(assertion => assertion.json()),
    //         risk_enabled: this._risk_enabled
    //     }
    // );
  }

  valid() {
    let valid = true;
    valid = valid && this._name !== "" && this._name !== undefined;
    valid = valid && this._questions.length > 0;
    this._questions.forEach(question => (valid = valid && question.valid()));
    return valid;
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    // TODO put it back later
    //valid = valid && data.hasOwnProperty("tags") && Array.isArray(data.tags);
    valid = valid && data.hasOwnProperty("questions");
    if (valid) {
      data.questions.forEach(assertion => {
        valid = valid && RestQuestion.validate(assertion);
      });
    }
    return valid;
  }
}

export class RestQuestionnaire {
  constructor(restData = null) {
    this._id = undefined;
    this._name = undefined;
    this._sections = [];
    this._low_risk_boundary = undefined;
    this._mid_risk_boundary = undefined;
    this._tags = [];
    this._risk_enabled = false;
    this._kb_ids = [];

    if (restData) {
      this._name = restData?.name;
      this._sections = restData?.sections;
      this._low_risk_boundary = restData?.low_risk_boundary;
      this._mid_risk_boundary = restData?.mid_risk_boundary;
      this._tags = restData?.tag_ids;
      this._risk_enabled = restData?.risk_enabled || false;
      this._id = restData?.id;
      this._kb_ids = restData?.kb_ids || [];
    }
    this.json = this.json.bind(this);
  }

  get name() {
    return this._name;
  }

  get sections() {
    return this._sections;
  }

  set name(name) {
    this._name = name;
  }

  set sections(sections) {
    this._sections = sections;
  }

  get tags() {
    return this._tags;
  }

  set tags(tags) {
    this._tags = tags;
  }

  get kb_ids() {
    return this._kb_id;
  }

  set kb_ids(kb) {
    this._kb_id = kb;
  }

  get low_risk_boundary() {
    return this._low_risk_boundary;
  }

  set low_risk_boundary(bound) {
    this._low_risk_boundary = bound;
  }

  get mid_risk_boundary() {
    return this._mid_risk_boundary;
  }
  set mid_risk_boundary(bound) {
    this._mid_risk_boundary = bound;
  }

  get risk_enabled() {
    return this._risk_enabled;
  }
  set risk_enabled(en) {
    this._risk_enabled = en;
  }

  json() {
    return {
      id: this._id,
      name: this._name,
      sections: (this._sections ?? []).map(section => section?.id),
      low_risk_boundary: this._low_risk_boundary,
      mid_risk_boundary: this._mid_risk_boundary,
      tag_ids: this._tags,
      risk_enabled: this._risk_enabled,
      kb_ids: this._kb_ids
    };
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    valid = valid && data.hasOwnProperty("sections") && Array.isArray(data.sections);
    // data.questions.forEach(
    //     question => {
    //         valid = valid && RestQuestion.validate(question);
    //     }
    // );
    return valid;
  }
}
