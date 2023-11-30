import BackendService from "./backendService";
import { QUESTIONNAIRES, QUESTIONNAIRES_NOTIFICATION } from "constants/restUri";

export class RestQuestionnairesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.search = this.search.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.bulkDelete = this.bulkDelete.bind(this);
  }

  list(filter = {}) {
    let url = QUESTIONNAIRES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = QUESTIONNAIRES.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = QUESTIONNAIRES.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = QUESTIONNAIRES.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, questionnaire) {
    let url = QUESTIONNAIRES.concat("/").concat(id.toString());
    let postData = questionnaire.json();
    postData.id = id;
    return this.restInstance.put(url, postData, this.options);
  }

  create(questionnaire) {
    let postData = questionnaire.json();
    return this.restInstance.post(QUESTIONNAIRES, postData, this.options);
  }

  bulkDelete(ids) {
    return this.restInstance.delete(QUESTIONNAIRES, { ...this.options, data: ids });
  }
}

export class QuestionnairesNotifyService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(data) {
    return this.restInstance.post(QUESTIONNAIRES_NOTIFICATION, data, this.options);
  }
}
