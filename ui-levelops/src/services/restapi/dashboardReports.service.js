import BackendService from "./backendService";
import { DASHBOARD_REPORTS, FILE_UPLOAD } from "constants/restUri";
import { get } from "lodash";

export class DashboardReportsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this.create = this.create.bind(this);
    this.update = this.update.bind(this);
    this.delete = this.delete.bind(this);
    this.upload = this.upload.bind(this);
  }

  list(filter = {}) {
    let url = DASHBOARD_REPORTS.concat("/list");
    return this.restInstance.post(url, filter, this.options);
  }

  get(dashboardId) {
    let url = DASHBOARD_REPORTS.concat("/").concat(dashboardId);
    return this.restInstance.get(url, this.options);
  }

  create(item) {
    return this.restInstance.post(DASHBOARD_REPORTS, item, this.options);
  }

  update(id, dashboard) {
    const url = DASHBOARD_REPORTS.concat("/").concat(id.toString());
    return this.restInstance.put(url, dashboard, this.options);
  }

  delete(dashboardId) {
    let url = DASHBOARD_REPORTS.concat("/").concat(dashboardId);
    return this.restInstance.delete(url, this.options);
  }

  upload(id, file, data) {
    const dashboardId = get(data, ["dashboard_id"], "0");
    const reportName = get(data, ["report_name"], "report.pdf");
    let url = FILE_UPLOAD.concat(`/dashboards/${dashboardId}`);
    let formData = new FormData();
    formData.append("file", file, reportName);
    let options = this.options;
    options.headers = { "Content-Type": "multipart/formdata" };
    return this.restInstance.post(url, formData, options);
  }
}
