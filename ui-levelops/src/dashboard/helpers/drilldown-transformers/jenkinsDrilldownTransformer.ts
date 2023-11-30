import { JENKINS_REPORTS } from "./../../constants/applications/names";
import { genericDrilldownTransformer } from "./genericDrilldownTransformer";
import { valuesToFilters } from "dashboard/constants/constants";
import { set } from "lodash";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { JENKINS_AZURE_REPORTS } from "dashboard/constants/applications/names";
import moment from "moment";

export const jenkinsDrilldownTransformer = (drillDownData: any) => {
  const { drillDownProps, widget } = drillDownData;
  const { x_axis } = drillDownProps || {};
  const reportValuesToFilters = getWidgetConstant(widget?.type, ["valuesToFilters"]);

  const genericResult = genericDrilldownTransformer(drillDownData);
  const across = genericResult?.acrossValue || "";

  // Handle integration_ids
  if (JENKINS_AZURE_REPORTS.includes(widget?.type)) {
    // Some Jenkins reports require the integration_ids filter
    // field to be removed, but some require them to be moved to
    // a filter called cicd_integration_ids before they are removed.
    const integration_ids = genericResult?.filters?.filter?.integration_ids;
    if (integration_ids) {
      genericResult.filters.filter.cicd_integration_ids = integration_ids;
      genericResult.filters.filter.integration_ids = integration_ids;
    }
  }

  // x axis
  const reportSpecificFilter = reportValuesToFilters?.[across];
  const globalFilter = valuesToFilters[across as keyof typeof valuesToFilters];
  const requestFilter = reportSpecificFilter || globalFilter || across;
  const handleTrendAcross = (timeValue: string) => {
    // across === "trend" is a special case where filter value should not be [x_axis]
    let filterName = reportSpecificFilter || "start_time";

    let timeRangeFilter = {
      $gt: `${+timeValue}`,
      $lt: `${+timeValue + 86399}`
    };

    const interval = widget?.query?.interval;
    if (interval) {
      timeRangeFilter = {
        $gt: `${moment
          .unix(+timeValue)
          .utc()
          .startOf(interval)
          .unix()}`,
        $lt: `${moment
          .unix(+timeValue)
          .utc()
          .endOf(interval)
          .unix()}`
      };
    }
    if (
      [
        JENKINS_REPORTS.JOB_CONFIG_CHANGE_COUNTS,
        JENKINS_REPORTS.JOB_CONFIG_CHANGE_COUNTS_STAT,
        JENKINS_REPORTS.JOB_CONFIG_CHANGE_COUNTS_TREND
      ].includes(widget?.type)
    ) {
      timeRangeFilter = {
        $gt: `${moment
          .unix(+timeValue)
          .utc()
          .startOf("day")
          .unix()}`,
        $lt: `${moment
          .unix(+timeValue)
          .utc()
          .endOf("day")
          .unix()}`
      };
    }
    set(genericResult, ["filters", "filter", filterName], timeRangeFilter);
  };

  if (x_axis && x_axis.constructor === Object) {
    if (across === "trend") {
      handleTrendAcross(x_axis.value);
    } else {
      x_axis.value && set(genericResult, ["filters", "filter", requestFilter], [x_axis.value]);
    }
  } else if (x_axis && typeof x_axis === "string") {
    if (across === "trend") {
      handleTrendAcross(x_axis);
    } else {
      // Default handling for string x_axis.
      set(genericResult, ["filters", "filter", requestFilter], [x_axis]);
    }
  }

  return genericResult;
};
