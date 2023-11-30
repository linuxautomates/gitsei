export class RestAnswer {
  constructor(restData = null) {
    this._question_id = undefined;
    this._assertions = [];
  }
}

export class RestQuizCreate {
  constructor() {
    this._questionnaire_template_id = undefined;
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
    this._product_id = undefined;

    this.json = this.json.bind(this);
  }

  get questionnaire_template_id() {
    return this._questionnaire_template_id;
  }

  get product_id() {
    return this._product_id;
  }

  set product_id(id) {
    this._product_id = id;
  }

  set questionnaire_template_id(id) {
    this._questionnaire_template_id = id;
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
      qtemplate_id: this._questionnaire_template_id,
      work_item_id: this._work_item_id,
      target_email: this._target_email,
      sender_email: this._sender_email,
      artifact: this._artifact,
      integration_url: this._integration_url,
      integration_application: this._integration_application,
      comm_channel: this._comm_channel,
      message_template_id: this._comm_template_id,
      additional_info: this._additional_info,
      product_id: this._product_id
    };
  }
}

export class RestQuiz {
  static COMMS = Object.freeze(["email", "slack"]);

  constructor(restData = null) {
    this._id = undefined;
    this._work_item_id = undefined;
    this._target_email = undefined;
    this._sender_email = undefined;
    this._assignment_message = undefined;
    this._questionnaire_id = undefined;
    this._policy_name = undefined;
    this._artifact = undefined;
    this._integration_url = undefined;
    this._integration_application = undefined;
    this._comments = undefined;
    this._completed = undefined;
    this._qtemplate_name = undefined;
    this._section_responses = [];
    this._current_score = undefined;
    this._sections = [];
    this._total_questions = undefined;
    this._answered_questions = undefined;
    this._product_id = undefined;
    this._generation = undefined;
    this._kb_ids = [];
    this.assertion = this.assertion.bind(this);
    this.updateAssertion = this.updateAssertion.bind(this);

    if (restData) {
      this._id = restData?.id;
      this._work_item_id = restData?.work_item_id;
      this._target_email = restData?.target_email;
      this._sender_email = restData?.sender_email;
      this._assignment_message = restData?.assignment_message;
      this._questionnaire_id = restData?.qtemplate_id;
      this._qtemplate_name = restData?.qtemplate_name;
      this._policy_name = restData?.policy_name;
      this._artifact = restData?.artifact;
      this._integration_application = restData?.integration_application;
      this._integration_url = restData?.integration_url;
      this._comments = restData?.comments;
      this._completed = restData?.completed;
      this._section_responses = restData?.section_responses || [];
      this._sections = restData?.sections || [];
      this._total_questions = restData?.total_questions;
      this._answered_questions = restData?.answered_questions;
      this._product_id = restData?.product_id;
      this._generation = restData?.generation;
      this._kb_ids = restData?.kb_ids || [];
    }

    // now add all the stupid logic about questions and assertions here
    let totalAssertions = 0;
    this._sections.forEach(section => {
      totalAssertions = totalAssertions + section.questions.length;
      let answer = {
        section_id: section.id,
        answers: section.questions.map(assert => ({
          question_id: assert.id,
          //response:assert.type==="multi-select"?[]:{value:"",score:0},
          responses: assert.type === "multi-select" || assert.type === "file upload" ? [] : [{ value: "", score: 0 }],
          comments: [],
          answered: false
        }))
      };
      let responses = this._section_responses.filter(answer => answer.section_id === section.id);
      if (responses.length === 0) {
        let ans = this._section_responses;
        ans.push(answer);
        this._section_responses = ans;
      } else {
        // question is present in the quiz. lets check and add default assertions
        let recordedAssertions = responses[0].answers;
        answer.answers.forEach(assert => {
          if (recordedAssertions.filter(a => a.question_id === assert.question_id).length === 0) {
            recordedAssertions.push(assert);
          }
        });
        // now push this into the quiz object and pray
        let allAnswers = this._section_responses;
        for (let i = 0; i < allAnswers.length; i++) {
          if (allAnswers[i].section_id === section.id) {
            allAnswers[i].answers = recordedAssertions;
          }
        }
        this._section_responses = allAnswers;
      }
    });
    this._total_questions = totalAssertions;
  }

  get kb_ids() {
    return this._kb_ids;
  }
  set kb_ids(ids) {
    this._kb_ids = ids;
  }

  get generation() {
    return this._generation;
  }

  set generation(gen) {
    this._generation = gen;
  }

  get id() {
    return this._id;
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

  set assignment_message(message) {
    this._assignment_message = message;
  }

  get assignment_message() {
    return this._assignment_message;
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

  get questionnaire_id() {
    return this._questionnaire_id;
  }

  set questionnaire_id(id) {
    this._questionnaire_id = id;
  }

  set qtemplate_name(name) {
    this._qtemplate_name = name;
  }

  get qtemplate_name() {
    return this._qtemplate_name;
  }

  get section_responses() {
    return this._section_responses;
  }

  set section_responses(answers) {
    this._section_responses = answers;
  }

  get completed() {
    return this._completed;
  }

  set completed(completed) {
    this._completed = completed;
  }

  get comments() {
    return this._comments;
  }

  set comments(comments) {
    this._comments = comments;
  }

  get current_score() {
    return this._current_score;
  }

  set current_score(score) {
    this._current_score = score;
  }

  get sections() {
    return this._sections;
  }

  set sections(q) {
    this._sections = q;
  }

  get total_questions() {
    return this._total_questions;
  }

  set total_questions(total) {
    this._total_questions = total;
  }

  get answered_questions() {
    return this._answered_questions;
  }

  set answered_questions(ans) {
    this._answered_questions = ans;
  }

  get product_id() {
    return this._product_id;
  }

  set product_id(id) {
    this._product_id = id;
  }

  json() {
    return {
      id: this._id,
      generation: this._generation,
      //generation: "1234",
      work_item_id: this._work_item_id,
      questionnaire_id: this._questionnaire_id,
      qtemplate_id: this._questionnaire_id,
      product_id: this._product_id,
      target_email: this._target_email,
      sender_email: this._sender_email,
      section_responses: this._section_responses,
      comments: this._comments,
      completed: this._completed,
      current_score: this._current_score,
      sections: this._sections,
      assignment_message: this._assignment_message,
      total_questions: this._total_questions,
      answered_questions: this._answered_questions,
      integration_url: this._integration_url,
      integration_application: this._integration_application,
      artifact: this._artifact,
      qtemplate_name: this._qtemplate_name,
      policy_name: this._policy_name,
      kb_ids: this._kb_ids
    };
  }

  assertion(sectionId, questionId) {
    if (!sectionId || !questionId) {
      return null;
    }
    for (let i = 0; i < this._section_responses.length; i++) {
      if (this._section_responses[i].section_id.toString() === sectionId.toString()) {
        let answer = this._section_responses[i];
        for (let j = 0; j < answer.answers.length; j++) {
          if (answer.answers[j].question_id.toString() === questionId.toString()) {
            return answer.answers[j];
          }
        }
      }
    }
    return null;
  }

  updateAssertion(sectionId, questionId, assertion) {
    for (let i = 0; i < this._section_responses.length; i++) {
      if (this._section_responses[i].section_id.toString() === sectionId.toString()) {
        let answer = this._section_responses[i];
        for (let j = 0; j < answer.answers.length; j++) {
          if (answer.answers[j].question_id.toString() === questionId.toString()) {
            answer.answers[j] = assertion;
            break;
          }
        }
      }
    }
  }

  static validate(data) {
    let valid = true;
    //valid = valid && data.hasOwnProperty("work_item_id");
    // valid = valid && data.hasOwnProperty("sender_email");
    //valid = valid && data.hasOwnProperty("target_email");
    //valid = valid && data.hasOwnProperty("policy_name");
    //valid = valid && data.hasOwnProperty("integration_url");
    //valid = valid && data.hasOwnProperty("integration_application");
    // valid = valid && data.hasOwnProperty("artifact");
    //valid = valid && data.hasOwnProperty("assignment_message");
    //valid = valid && data.hasOwnProperty("comments");
    valid = valid && data.hasOwnProperty("qtemplate_id");
    valid = valid && data.hasOwnProperty("qtemplate_name");
    //valid = valid && data.hasOwnProperty("completed");
    valid = valid && data.hasOwnProperty("section_responses") && Array.isArray(data.section_responses);
    valid = valid && data.hasOwnProperty("sections") && Array.isArray(data.sections);
    return valid;
  }
}
