import { v1 as uuid } from "uuid";

import { RestDashboard, RestWidget } from "../../classes/RestDashboards";
import CompactReport from "../report/CompactReport";
import { Model } from "../Model";

// TODO: after merging all big PR's move RestWidget here.
class Widget extends Model {
  static newInstance(dashboard: RestDashboard, report: CompactReport, widgets = undefined) {
    if (!dashboard || !report) {
      return null;
    }

    if (report.supported_widget_types.length === 0) {
      console.error(report, "Set widget type for it");
      return null;
    }

    const widgetType = report.supported_widget_types[0];
    return RestWidget.newInstance(dashboard, uuid(), widgetType, "", false, report.key as any, widgets);
  }
}

export default Widget;
