import BackendService from "./backendService";
import { ASSESSMENTS_DOWNLOAD } from "constants/restUri";

export class AssessmentsDownloadService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
  }

  list(filter = {}) {
    let options = { responseType: "blob" };
    return this.restInstance.post(ASSESSMENTS_DOWNLOAD, filter, options);
  }
}
