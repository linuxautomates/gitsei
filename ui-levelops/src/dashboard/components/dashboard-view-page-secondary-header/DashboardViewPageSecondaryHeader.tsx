import React, { useCallback, useMemo } from "react";
import { AntIcon, AntSelect, AntText } from "../../../shared-resources/components";
import SelectRestapi from "../../../shared-resources/helpers/select-restapi/select-restapi";
import { useDispatch, useSelector } from "react-redux";
import { Popover } from "antd";
import DashboardHeaderDateRangePickerWrapper from "./DateRangePickerHeaderWrapper";
import { getDashboardTimeRangeDateValue, timeStampToValue } from "./helper";
import { ProfileUnitsOptions } from "./constants";
import { dashboardDefault, newDashboardUpdate, tenantStateGet } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import "./dashboard-view-page-secondary-header.style.scss";
import { DashboardSearchDropdown } from "dashboard/pages/dashboard-drill-down-preview/components/dashboardSearchDropdown";
import {
  DASHBOARDS_TITLE,
  DASHBOARD_LIST_COUNT,
  DEFAULT_DASHBOARD_KEY,
  SECURITY,
  TENANT_STATE
} from "dashboard/constants/constants";
import { DashboardHeaderConfigType } from "dashboard/dashboard-types/Dashboard.types";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { useHistory } from "react-router-dom";
import { getDashboardTimeRangeIfPreCalc } from "../dashboard-header/helper";
import { TENANTS_USING_PRE_CALC } from "../dashboard-header/constants";
import LocalStoreService from "services/localStoreService";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";
interface DashboardViewPageSecondaryHeaderProps {
  dashboardId: string;
  metaData: any;
  onFilerValueChange: (value: any, key: string) => void;
  popOverVisible?: boolean;
  setPopOverVisible: (value: any) => void;
}

const DashboardViewPageSecondaryHeaderComponent: React.FC<DashboardViewPageSecondaryHeaderProps> = ({
  dashboardId,
  metaData,
  onFilerValueChange,
  popOverVisible,
  setPopOverVisible
}) => {
  const dispatch = useDispatch();
  const dashboardsListState = useParamSelector(getGenericRestAPISelector, {
    uri: "dashboards",
    method: "list",
    uuid: DASHBOARD_LIST_COUNT
  });
  const history = useHistory();
  const dashboard = useSelector(selectedDashboard);

  const datePickerCloseHandler = () => {
    setPopOverVisible(false);
  };

  const ls = new LocalStoreService();
  const content = useMemo(() => {
    return (
      <DashboardHeaderDateRangePickerWrapper
        closeHandler={datePickerCloseHandler}
        onFilerValueChange={onFilerValueChange}
        metaData={metaData}
        dashboardTimeRangeOptions={getDashboardTimeRangeIfPreCalc()}
        disableCustom={TENANTS_USING_PRE_CALC.includes(ls.getUserCompany() || "")}
      />
    );
  }, [metaData, onFilerValueChange]);

  const dashboardTimeGtValue = useMemo(() => {
    return typeof metaData?.dashboard_time_range_filter === "string"
      ? getDashboardTimeRangeDateValue(metaData?.dashboard_time_range_filter || "last_30_days", "$gt")
      : timeStampToValue(metaData?.dashboard_time_range_filter?.[`$gt`] || {});
  }, [metaData]);

  const dashboardTimeLtValue = useMemo(() => {
    return typeof metaData?.dashboard_time_range_filter === "string"
      ? getDashboardTimeRangeDateValue(metaData?.dashboard_time_range_filter || "last_30_days", "$lt")
      : timeStampToValue(metaData?.dashboard_time_range_filter?.[`$lt`] || {});
  }, [metaData]);

  const typeOfDashboardTimeFilterString = typeof metaData?.dashboard_time_range_filter === "string";

  const dashboardHeaderConfig: DashboardHeaderConfigType =
    dashboard?.type === SECURITY
      ? { dashCount: -1, dashboardTitle: dashboard?.name || DASHBOARDS_TITLE, style: { margin: "2rem 0rem" } }
      : {
          dashCount: get(dashboardsListState, ["data", "_metadata", "total_count"], 0),
          dashboardTitle: dashboard?.name
        };

  const handleSetDefault = useCallback(
    (id: string) => {
      let dashDefault = false;
      if (dashboardId === id) {
        dashDefault = true;
      }
      dispatch(dashboardDefault(DEFAULT_DASHBOARD_KEY));
      dispatch(newDashboardUpdate(dashboardId, { default: dashDefault }));
    },
    [dashboardId]
  );

  const showOnLeft =
    isDashboardTimerangeEnabled(metaData) || metaData?.effort_investment_profile || metaData?.effort_investment_unit;
  return (
    <div
      className="dashboard-view-secondary-header"
      style={{ justifyContent: "space-between", width: !showOnLeft ? "80%" : "100%" }}>
      <div className="dashboard-view-secondary-header-right flex justify-end">
        <div className="flex justify-center align-baseline">
          {(metaData?.effort_investment_profile || metaData?.effort_investment_unit) && (
            <AntText color="#595959" className="view-text-style ml-15">
              Alignment Profile :
            </AntText>
          )}
          {metaData?.effort_investment_profile && (
            <div className="effort-investment-profile-container">
              <SelectRestapi
                suffixIcon={<AntIcon type="down" style={{ fill: "solid" }} />}
                value={metaData?.effort_investment_profile_filter || ""}
                placeholder=""
                showSpinnerWhenLoading={true}
                uri="ticket_categorization_scheme"
                method="list"
                useOnSelect={true}
                className="header-ei-profile-select"
                searchField={"name"}
                createOption={false}
                doNotUseOnChange={true}
                dropdownClassName="dashboard-ou-dropdown"
                onChange={(value: any) => {
                  onFilerValueChange(value.value, "effort_investment_profile_filter");
                }}
                allowClear={false}
              />
            </div>
          )}
          {metaData?.effort_investment_unit && (
            <div className="effort-investment-unit-container">
              Units:
              <AntSelect
                suffixIcon={<AntIcon type="down" style={{ fill: "solid" }} />}
                className="profile-unit ml-16"
                dropdownClassName="dashboard-ou-dropdown"
                defaultValue={
                  ProfileUnitsOptions.find((item: any) => item.value === metaData?.effort_investment_unit_filter)?.label
                }
                getPopupContainer={(trigger: any) => trigger.parentNode}
                value={metaData?.effort_investment_unit_filter}
                options={ProfileUnitsOptions}
                onSelect={(value: any) => onFilerValueChange(value, "effort_investment_unit_filter")}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DashboardViewPageSecondaryHeaderComponent;
