import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import queryParser from "query-string";
import { useDispatch, useSelector } from "react-redux";
import { useLocation } from "react-router-dom";
import { get, isEqual } from "lodash";
import DemoScoreCardDashboardHeader from "../component/DemoScoreCardDashboardHeader";
import { devProductivityWidgets, scoreWidgetType } from "dashboard/pages/scorecard/constants";
import { DevWidgetTimeRangeContext } from "dashboard/pages/context";
import DemoDevWidgetContainer from "./DemoDevWidgetContainer";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { _ouProdScoreSelect, orgUnitGetRestDataSelect } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import "./demoDevProductivityDashboard.styles.scss";
import { Icon, Spin } from "antd";
import { AntSelectComponent } from "../../../../shared-resources/components/ant-select/ant-select.component";
import { dashboardsGet } from "reduxConfigs/actions/restapi";
import { useDemoDashboardDataId } from "custom-hooks/useDemoDashboardDataKey";
import DemoOUScoreOverViewComponent from "../component/DemoOUScoreOverViewComponent";
import DemoRelativeScoreChart from "../component/DemoRelativeScoreChart";
import { supportedSections } from "./constants";
import { getOUOptionsAction } from "reduxConfigs/actions/restapi/OrganizationUnit.action";

const DemoDevProductivityDashboard: React.FC = () => {
  const location = useLocation();
  const { ou_id, ou_uuid, interval, id, dashboardId, index } = queryParser.parse(location.search);

  const dispatch = useDispatch();
  const [dashboardTimeRange, setDashboardTimeRange] = useState<any>(interval || "last_quarter");
  const [relativeScoreFilters, setRelativeScoreFilters] = useState<{ [key: string]: string }>({});
  const [relativeScoreDataPeriod, setRelativeScoreDataPeriod] = useState<string>("relativeScoreDataMonthly");
  const [relativeScorePeriod, setRelativeScorePeriod] = useState<string>("Month");
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    dispatch(getOUOptionsAction("organization_unit_management", "list", "dashboard_ou_options"));
  }, []);

  useEffect(() => {
    setTimeout(() => {
      setLoading(false);
    }, 1000);
  });

  const demoDataKey = useDemoDashboardDataId(id);

  const widgetData = useSelector(state =>
    get(
      state,
      [
        "restapiReducer",
        "selected-dashboard",
        "metadata",
        "demo_data",
        id as string,
        "data",
        demoDataKey as string,
        "data"
      ],
      {}
    )
  );

  const data = widgetData[Number(index)];

  const relativeScoreTransformedData = useSelector(state =>
    get(
      state,
      [
        "restapiReducer",
        "selected-dashboard",
        "metadata",
        "demo_data",
        id as string,
        "data",
        demoDataKey as string,
        relativeScoreDataPeriod
      ],
      {}
    )
  );

  const trellisScoreDevData = useSelector(state =>
    get(
      state,
      [
        "restapiReducer",
        "selected-dashboard",
        "metadata",
        "demo_data",
        id as string,
        "data",
        demoDataKey as string,
        "trellisScoreDevData"
      ],
      {}
    )
  );

  const orgUnit: RestOrganizationUnit = useParamSelector(orgUnitGetRestDataSelect, {
    id: ou_id
  });

  useEffect(() => {
    dispatch(dashboardsGet(dashboardId));
  }, [dashboardId]); // eslint-disable-line react-hooks/exhaustive-deps

  const onRelativeFilterChange = (value: string, type: string) => {
    if (type === "agg_interval" && value === "Quarter") {
      setRelativeScorePeriod("Quarter");
      setRelativeScoreDataPeriod("relativeScoreDataQuarterly");
    }
    if (type === "agg_interval" && value === "Month") {
      setRelativeScorePeriod("Month");
      setRelativeScoreDataPeriod("relativeScoreDataMonthly");
    }
  };

  const relativeScoreChartExtraFilters = useMemo(
    () => (
      <>
        <Icon type="clock-circle" />
        <AntSelectComponent
          className="relative-core-dropdown"
          value={relativeScorePeriod}
          options={["Quarter", "Month"]}
          onChange={(value: string) => onRelativeFilterChange(value, "agg_interval")}
        />
      </>
    ),
    [relativeScoreFilters, onRelativeFilterChange]
  );

  return (
    <>
      {loading ? (
        <div className="scorecard-container-spinner">
          <Spin />
        </div>
      ) : (
        <div className="demo-dev-productivity-dashboard-container">
          <DemoScoreCardDashboardHeader
            dashboardTimeRange={dashboardTimeRange}
            dashboardTitle={`${orgUnit?.name ?? ""} Overview`}
          />
          <div className="dev-productivity-widget-list">
            <DemoOUScoreOverViewComponent data={data} />
            <div className="widgets">
              {devProductivityWidgets.map(widget =>
                widget.widget_type === scoreWidgetType.ORGANIZATION_UNIT_PRODUCTIVITY_SCORE ? (
                  <DemoRelativeScoreChart
                    extraFilters={relativeScoreChartExtraFilters}
                    data={relativeScoreTransformedData}
                    relativeScoreDataPeriod={relativeScoreDataPeriod}
                  />
                ) : (
                  <DevWidgetTimeRangeContext.Provider
                    value={{
                      dashboardTimeRange: dashboardTimeRange
                    }}>
                    <DemoDevWidgetContainer
                      widget={widget}
                      interval={interval as string}
                      ou_uuid={(ou_uuid ?? "") as string}
                      supportedSections={supportedSections}
                      trellisScoreDevData={trellisScoreDevData}
                    />
                  </DevWidgetTimeRangeContext.Provider>
                )
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default DemoDevProductivityDashboard;
