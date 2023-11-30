import BackendService from "./backendService";
import { QUIZ, FILE_UPLOAD } from "constants/restUri";

export class RestQuizService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.search = this.search.bind(this);
    this.get = this.get.bind(this);
    this.delete = this.delete.bind(this);
    this.update = this.update.bind(this);
    this.create = this.create.bind(this);
    this.upload = this.upload.bind(this);
  }

  list(filter = {}) {
    let url = QUIZ.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  search(filter = {}) {
    let url = QUIZ.concat("/search");
    return this.restInstance.post(url, filter, this.options);
  }

  get(id) {
    let url = QUIZ.concat("/").concat(id);
    return this.restInstance.get(url, this.options);
  }

  delete(id) {
    let url = QUIZ.concat("/").concat(id);
    return this.restInstance.delete(url, this.options);
  }

  update(id, quiz) {
    let url = QUIZ.concat("/").concat(id.toString());
    let postData = quiz.json();
    return this.restInstance.put(url, postData, this.options);
  }

  create(quiz) {
    let postData = quiz.json();
    return this.restInstance.post(QUIZ, postData, this.options);
  }

  upload(id, file) {
    let [quizId, assertionId] = id.split(":");
    let url = FILE_UPLOAD.concat(`/quiz/${quizId}/assertion/${assertionId}`);
    let formData = new FormData();
    formData.append("file", file);
    let options = this.options;
    options.headers = { "Content-Type": "multipart/formdata" };
    return this.restInstance.post(url, formData, options);
  }
}
