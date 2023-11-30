import React from "react";
import { unset } from "lodash";
import { Link } from "react-router-dom";
import { EmptyWidgetPreviewArgsType } from "model/report/dev-productivity/baseDevProductivityReports.constants";
import { ReactNode } from "react";
import { API_ERROR_CODES } from "reduxConfigs/constants/common.constants";
import { WebRoutes } from "routes/WebRoutes";
import EmptyWidgetPreview from "configurable-dashboard/components/widget-preview/custom-preview/EmptyWidgetPreview";
import { REPORT_REQUIRES_OU } from "constants/formWarnings";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import WidgetCardTitle from "dashboard/components/widgets/WidgetCardTitle";
import { Icon, Tooltip } from "antd";

export const getGenericFilters = (data: any) => {
  const { finalFilters, ou_ids } = data;
  let modifiedFilters = {
    ...finalFilters
  };
  const interval = finalFilters?.interval || "LAST_QUARTER";
  unset(modifiedFilters, "interval");
  modifiedFilters = {
    ...modifiedFilters,
    filter: {
      ...(modifiedFilters?.filter || {}),
      interval,
      ou_ref_ids: ou_ids.length ? ou_ids : undefined
    }
  };
  return modifiedFilters;
};

/**
 * This function return widget content on the basis of error-code
 * @param args : errorCode which is a number
 * @return ReactNode
 */
export const emptyWidgetPreviewFunc = (args: EmptyWidgetPreviewArgsType): ReactNode => {
  const { errorCode, chartType } = args;
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
    case API_ERROR_CODES.BAD_REQUEST:
    case API_ERROR_CODES.UN_AUTHORIZED:
      return (
        <EmptyWidgetPreview
          message={errorCode === 403 ? "You do not have permissions to view this report." : REPORT_REQUIRES_OU}
          isStat={chartType === ChartType.STATS}
          notificationMessage="Failed to get raw stats data since user does not have the access to view the Trellis Score/Raw Stats data"
        />
      );
    default:
      return <EmptyWidgetPreview />;
  }
};

export const getWidgetTitle = (args: any) => {
  return (
    <>
      <WidgetCardTitle {...args} />
      <span className="last-updated-at">Last Updated at: {args.resultTime}</span>
    </>
  );
};

export const GetInfoIcon = () => {
  return (
    <Tooltip
      placement="bottom"
      title={
        <span>There could be a delay in generating this report, resulting in a lag of approximately 0-2 days.</span>
      }>
      <span className="cal-icon ml-5">
        <Icon type="info-circle" />
      </span>
    </Tooltip>
  );
};
