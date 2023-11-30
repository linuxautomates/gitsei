import { STORY_POINT_REPORT } from "constants/restUri";
import BackendService from "services/backendService";

export class StoryPointReportService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    return this.restInstance.post(STORY_POINT_REPORT, filter, this.options);
  }
}
