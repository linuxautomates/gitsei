import { engineerManagerType, engineerSectionType } from "dashboard/dashboard-types/engineerScoreCard.types";
import { sanitizeObject } from "utils/commonUtils";

export class RestDevProdEngineer {
  _name: string;
  _email: string;
  _manager: engineerManagerType;
  _section_responses: engineerSectionType[];
  _score: number;
  _org_user_id: string;
  _start_time: string;
  _end_time: string;
  _result_time: string;
  constructor(restData = {}) {
    this._name = "";
    this._email = "";
    this._manager = { full_name: "", email: "" };
    this._section_responses = [];
    this._score = 0;
    this._org_user_id = "";
    this._start_time = "";
    this._end_time = "";
    this._result_time = "";

    if (!!restData) {
      this._name = (restData || ({} as any)).full_name;
      this._email = (restData || ({} as any)).email;
      this._manager = (restData || ({} as any)).manager;
      this._score = (restData || ({} as any)).score;
      this._section_responses = (restData || ({} as any)).section_responses;
      this._org_user_id = (restData || ({} as any)).org_user_id;
      this._start_time = (restData || ({} as any)).start_time;
      this._end_time = (restData || ({} as any)).end_time;
      this._result_time = (restData || ({} as any)).result_time;
    }
  }

  get name() {
    return this._name;
  }

  set name(gname: string) {
    this._name = gname;
  }

  get email() {
    return this._email;
  }

  set email(gname: string) {
    this._email = gname;
  }

  get score() {
    return this._score;
  }

  get manager() {
    return this._manager;
  }

  set manager(manager: engineerManagerType) {
    this._manager = manager;
  }

  get section_responses() {
    return this._section_responses;
  }

  set section_responses(section_responses: engineerSectionType[]) {
    this._section_responses = section_responses;
  }

  get org_user_id() {
    return this._org_user_id;
  }

  set org_user_id(org_user_id) {
    this.org_user_id = org_user_id;
  }

  get start_time() {
    return this._start_time;
  }

  set start_time(start_time) {
    this._start_time = start_time;
  }

  get end_time() {
    return this._end_time;
  }

  set end_time(end_time) {
    this._end_time = end_time;
  }

  get result_time() {
    return this._end_time;
  }

  set result_time(result_time) {
    this._result_time = result_time;
  }

  get json() {
    return sanitizeObject({
      name: this._name,
      email: this._email,
      managers: this._manager,
      section_responses: this._section_responses,
      score: this._score,
      start_time: this._start_time?.toString(),
      end_time: this._end_time?.toString(),
      result_time: this._result_time?.toString()
    });
  }
}
