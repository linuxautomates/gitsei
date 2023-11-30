import React, { useEffect, useState, useMemo } from "react";
import { RouteComponentProps, useLocation } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { get, isEqual } from "lodash";
import queryParser from "query-string";
import {
  getDashboardTimeGtValue,
  getDashboardTimeLtValue
} from "dashboard/components/dashboard-view-page-secondary-header/helper";
import DemoScoreCardDashboardHeader from "dashboard/pages/demo-dev-productivity-report/component/DemoScoreCardDashboardHeader";
import "../../scorecard/containers/scoreCardDashboardView.styles.scss";
import { Icon, Spin } from "antd";
import { AntSelectComponent } from "shared-resources/components/ant-select/ant-select.component";
import { dashboardsGet } from "reduxConfigs/actions/restapi";
import { useDemoDashboardDataId } from "custom-hooks/useDemoDashboardDataKey";
import DemoEngineerProfileCard from "../components/DemoEngineerProfileCard";
import DemoPRActivityComponent from "dashboard/pages/dashboard-view/Demo-Dashboard/Widgets-Graph/PR-Activity/DemoPRActivityComponent";
import DemoRelativeScoreChart from "dashboard/pages/demo-dev-productivity-report/component/DemoRelativeScoreChart";
import DemoEngineerScoreOverViewCard from "../components/DemoEngineerScoreOverViewCard";
import { getOUOptionsAction, orgUnitDashboardList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";

const DemoScoreCardDashboardViewPage: React.FC<RouteComponentProps> = () => {
  const dispatch = useDispatch();
  const location = useLocation();
  const { interval, index, id, dashboardId, ou_uuid } = queryParser.parse(location.search);

  const [relativeScoreFilters, setRelativeScoreFilters] = useState<{ [key: string]: string }>({});
  const [dashboardTimeRange, setDashboardTimeRange] = useState<any>(interval || "last_quarter");
  const [relativeScoreDataPeriod, setRelativeScoreDataPeriod] = useState<string>("relativeScoreDataMonthly");
  const [relativeScorePeriod, setRelativeScorePeriod] = useState<string>("Month");
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedTrellisProfile, setSelectedTrellisProfile] = useState<any>();

  useEffect(() => {
    dispatch(dashboardsGet(dashboardId));
  }, [dashboardId]); // eslint-disable-line react-hooks/exhaustive-deps

  const ORG_TREE_LIST_UUID = "ORG_TREE_LIST";

  useEffect(() => {
    setTimeout(() => {
      setLoading(false);
    }, 1000);
  });

  useEffect(() => {
    dispatch(getOUOptionsAction("organization_unit_management", "list", "dashboard_ou_options"));
  }, []);

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

  const dashboardTimeGtValue = useMemo(() => getDashboardTimeGtValue(dashboardTimeRange), [dashboardTimeRange]);

  const dashboardTimeLtValue = useMemo(() => getDashboardTimeLtValue(dashboardTimeRange), [dashboardTimeRange]);

  const orgListOptions = useMemo(() => {
    return selectedTrellisProfile?.associated_ous?.length
      ? selectedTrellisProfile.associated_ous.map((org: { [key: string]: string }) => ({
          value: org.ou_id,
          label: org.ou_name
        }))
      : [];
  }, [selectedTrellisProfile]);

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

  const extras = useMemo(
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
    [relativeScoreFilters, onRelativeFilterChange, orgListOptions]
  );

  return (
    <>
      {loading ? (
        <div className="scorecard-container-spinner">
          <Spin />
        </div>
      ) : (
        <div className="scorecard-container">
          <DemoScoreCardDashboardHeader dashboardTitle="Developer Overview" dashboardTimeRange={dashboardTimeRange} />
          <div className={"scorecard-container-layout"}>
            <DemoEngineerProfileCard data={data} />
            {data && (
              <div className="scorecard-container-layout-body">
                <DemoPRActivityComponent
                  dashboardTimeGtValue={dashboardTimeGtValue}
                  dashboardTimeLtValue={dashboardTimeLtValue}
                  data={data}
                  isTrellisDemoWidget={true}
                />
                <DemoRelativeScoreChart
                  extraFilters={extras}
                  data={data?.[relativeScoreDataPeriod]}
                  relativeScoreDataPeriod={relativeScoreDataPeriod}
                />
                <DemoEngineerScoreOverViewCard data={data} />
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
};

export default DemoScoreCardDashboardViewPage;
