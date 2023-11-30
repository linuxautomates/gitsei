import React from "react";
import { RestWidget } from "classes/RestDashboards";
import { get } from "lodash";
import { Link } from "react-router-dom";
import { EmptyWidgetPreviewArgsType } from "model/report/dev-productivity/baseDevProductivityReports.constants";
import { ReactNode } from "react";
import { API_ERROR_CODES } from "reduxConfigs/constants/common.constants";
import { WebRoutes } from "routes/WebRoutes";
import { defaultColumns, getDeveloperRawStatColumns } from "../rawStatsTable.config";
import { EmptyWidget } from "shared-resources/components";

export const transformData = (data: any) => {
  const { apiData } = data;
  return { data: apiData || [] };
};

export const getChartProps = (widget: RestWidget, interval?: string) => {
  const userAttributes = get(widget, ["query", "userAttributes"], []);
  const { selected_columns } = widget;
  return {
    columns: getDeveloperRawStatColumns(userAttributes, selected_columns, interval, "developer-raw-stat"),
    defaultColumns: defaultColumns
  };
};

export const getFilters = (props: any) => {
  const { tempWidgetInterval } = props;
  return {
    filter: {
      ou_ref_ids: props.dashboardOuIds,
      interval: tempWidgetInterval ? tempWidgetInterval : props.filters.interval
    }
  };
};

/**
 * This function return widget content on the basis of error-code
 * @param args : errorCode which is a number
 * @return ReactNode
 */
export const individualRawStatEmptyWidgetPreviewFunc = (args: EmptyWidgetPreviewArgsType): ReactNode => {
  const { errorCode } = args;
  switch (errorCode) {
    case API_ERROR_CODES.NOT_FOUND:
      return (
        <div className="org-unit-association-error-container">
          <div className="org-unit-association-error">
            This Collection is not associated with any Trellis profile. Please{" "}
            <Link to={`${WebRoutes.trellis_profile.list()}`}>click here</Link> to associate it with a profile.
          </div>
        </div>
      );
    case API_ERROR_CODES.UN_AUTHORIZED:
      return (
        <EmptyWidget notificationMessage="Failed to get raw stats data since user does not have the access to view the Trellis Score/Raw Stats data" />
      );
    default:
      return <EmptyWidget />;
  }
};
