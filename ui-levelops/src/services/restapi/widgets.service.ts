import { RestWidget } from "classes/RestDashboards";
import { DASHBOARDS } from "constants/restUri";
import BackendService from "./backendService";

/**
 * Docs: https://levelops.atlassian.net/wiki/spaces/LEV/pages/1104642300/Dashboard+widgets+API
 */
export class WidgetsService extends BackendService {
  constructor() {
    super();
    this.list = this.list.bind(this);
    this.get = this.get.bind(this);
    this._create = this._create.bind(this);
    this._update = this._update.bind(this);
    this._delete = this._delete.bind(this);
    this.deleteBulk = this.deleteBulk.bind(this);
  }

  list(dashboardId: string, filters = {}) {
    return this.restInstance.post(`${DASHBOARDS}/${dashboardId}/widgets/list`, filters, this.options);
  }

  get(dashboardId: string, widgetId: string) {
    return this.restInstance.get(`${DASHBOARDS}/${dashboardId}/widgets/${widgetId}`, this.options);
  }

  _create(payload: { dashboard_id: string; widget: RestWidget }) {
    const { dashboard_id, widget } = payload;
    return this.restInstance.post(`${DASHBOARDS}/${dashboard_id}/widgets`, widget.json, this.options);
  }

  _update(payload: { dashboard_id: string; widget: RestWidget }) {
    const { dashboard_id, widget } = payload;
    return this.restInstance.put(`${DASHBOARDS}/${dashboard_id}/widgets/${widget.id}`, widget.json, this.options);
  }

  _delete(payload: { dashboard_id: string; widget_id: string }) {
    return this.restInstance.delete(`${DASHBOARDS}/${payload.dashboard_id}/widgets/${payload.widget_id}`, this.options);
  }

  deleteBulk(payload: { dashboard_id: string; widget_ids: string[] }) {
    return this.restInstance.delete(`${DASHBOARDS}/${payload.dashboard_id}/widgets`, {
      data: payload.widget_ids,
      ...this.options
    });
  }
}
