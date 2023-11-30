import React, { useContext, useEffect } from "react";
import { useDispatch } from "react-redux";
import { ORG_USER_SCHEMA_ID, orgUsersGenericSelector } from "reduxConfigs/selectors/orgUsersSelector";
import { OrgUserSchemaGet } from "reduxConfigs/actions/restapi/orgUserAction";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { WidgetTabsContext } from "../../../pages/context";
import { Form } from "antd";
import { AntSelect } from "../../../../shared-resources/components";
import { get } from "lodash";
import { WIDGET_CONFIGURATION_KEYS } from "../../../../constants/widgets";
import {
  DEV_PRODUCTIVITY_INTERVAL_OPTIONS,
  DEV_PRODUCTIVITY_INTERVAL_OPTIONS_FOR_RAW_STATS,
  OLD_INTERVAL
} from "./constants";
import { timeInterval } from "../../../constants/devProductivity.constant";
import { DEV_PRODUCTIVITY_REPORTS } from "dashboard/constants/applications/names";
import DevChildCheckboxFilter from "./dev-child-ou-checkbox";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";

interface DevProductivityFiltersProps {
  filters: any;
  application: string;
  reportType: string;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  onMetadataChange?: (value: any, type: any, reportType?: String) => void;
  metaData: any;
}

const DevProductivityFilters: React.FC<DevProductivityFiltersProps> = (props: DevProductivityFiltersProps) => {
  const { filters, application, reportType, onFilterValueChange, onMetadataChange, metaData } = props;
  const { isVisibleOnTab } = useContext(WidgetTabsContext);
  const showNewInterval = useHasEntitlements(Entitlement.SHOW_TRELIS_NEW_INTERVAL, EntitlementCheckType.AND);
  const dispatch = useDispatch();

  const userSchemaState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_schema",
    method: "get",
    id: ORG_USER_SCHEMA_ID
  });

  const getSelectValue = (filters: any) => {
    let interval = get(filters, ["interval"], timeInterval.LAST_QUARTER);
    //a fallback approach if users selected LAST_YEAR for interval filter then convert to new standard LAST_TWELVE_MONTHS
    if (interval === timeInterval.LAST_YEAR) {
      interval = timeInterval.LAST_TWELVE_MONTHS;
    }
    return interval;
  };

  const getSelectUserAttributeValues = (userAttributes: any) => {
    let userAttribute = get(userAttributes, ["userAttributes"], []);
    return userAttribute;
  };

  useEffect(() => {
    if (!get(userSchemaState, ["data", "fields"], []).length) {
      dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema"));
    }
    return () => {
      dispatch(restapiClear("org_users_schema", "create", -1));
    };
  }, []);

  const getOptions = () => {
    const data = get(userSchemaState, ["data", "fields"], []);
    const updatedData = data
      .filter((item: any) => !["full_name", "email", "start_date", "integration"].includes(item.key))
      .sort((a: any, b: any) => a.index - b.index)
      .map((item: any) => {
        return {
          value: item.key,
          label: item?.display_name
        };
      });
    return updatedData;
  };

  const getIntervalOptions = () => {
    if (!showNewInterval || reportType === DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT) {
      return OLD_INTERVAL;
    }
    return reportType === DEV_PRODUCTIVITY_REPORTS.INDIVIDUAL_RAW_STATS
      ? DEV_PRODUCTIVITY_INTERVAL_OPTIONS_FOR_RAW_STATS
      : DEV_PRODUCTIVITY_INTERVAL_OPTIONS;
  };

  const getIntervalValue = () => {
    return getSelectValue(filters);
  };

  return (
    <div data-testid="dashboard-graph-filters-component" className="configure-widget-filters">
      <div className="configure-widget-filters__half">
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
          <Form.Item key="interval" label={"Interval"}>
            <AntSelect
              showArrow={true}
              value={getIntervalValue()}
              options={getIntervalOptions()}
              mode={"single"}
              onChange={(value: any, options: any) => onFilterValueChange(value, "interval")}
            />
          </Form.Item>
        )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) &&
          reportType === DEV_PRODUCTIVITY_REPORTS.PRODUCTIVITY_SCORE_BY_ORG_UNIT && (
            <DevChildCheckboxFilter
              value={get(filters, ["is_immediate_child_ou"], false)}
              onFilterValueChange={onFilterValueChange}
            />
          )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && reportType === "individual_raw_stats_report" && (
          <Form.Item key="userAttributes" label={"Select User Attributes"}>
            <AntSelect
              showArrow={true}
              value={getSelectUserAttributeValues(filters)}
              options={getOptions()}
              mode="multiple"
              onChange={(value: any, options: any) => onFilterValueChange(value, "userAttributes")}
            />
          </Form.Item>
        )}
      </div>
    </div>
  );
};

export default React.memo(DevProductivityFilters);
