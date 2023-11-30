import BackendService from "./backendService";
import { NOTES } from "constants/restUri";

export class RestNotesService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.create = this.create.bind(this);
  }

  list(filter = {}) {
    let url = NOTES.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  create(note) {
    let postData = note.json();
    return this.restInstance.post(NOTES, postData, this.options);
  }
}
