import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import queryParser from "query-string";
import { useDispatch } from "react-redux";
import { useLocation } from "react-router-dom";
import { get, isEqual } from "lodash";
import ScoreCardDashboardHeader from "dashboard/components/dashboard-view-page-header/ScoreCardDashboardHeader";
import { devProductivityWidgets, scoreWidgetType } from "dashboard/pages/scorecard/constants";
import cx from "classnames";
import { DevWidgetTimeRangeContext } from "dashboard/pages/context";
import DevWidgetContainer from "./DevWidgetContainer";
import OUScoreOverViewComponent from "../component/OUScoreOverViewComponent";
import { ouScoreOverview } from "reduxConfigs/actions/restapi/trellisReportActions";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import {
  _ouProdScoreSelect,
  orgUnitGetRestDataSelect,
  orgUnitListDataState
} from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import "./devProductivityDashboard.styles.scss";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { Icon, Spin } from "antd";
import RelativeScoreChart from "../../scorecard/components/RelativeScoreChart";
import { AntSelectComponent } from "../../../../shared-resources/components/ant-select/ant-select.component";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { OrganizationUnitList, ouProductivityScore } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { OU_PROD_SCORE_ID } from "../../../../configurations/pages/Organization/Constants";
import { OU_DASHBOARD_LIST_ID } from "../../../components/dashboard-view-page-secondary-header/OUSelectorDropdownContainer";
import { getInterval } from "dashboard/graph-filters/components/DevProductivityFilters/constants";
import { getIsStandaloneApp } from "helper/helper";

const DevProductivityDashboard: React.FC = () => {
  const location = useLocation();
  const { ou_id, ou_uuid, interval } = queryParser.parse(location.search);
  const dispatch = useDispatch();
  const [dashboardTimeRange, setDashboardTimeRange] = useState<any>(interval || "last_quarter");
  const [relativeScoreFilters, setRelativeScoreFilters] = useState<{ [key: string]: string }>({});
  const apiFiltersRef = useRef<basicMappingType<any>>();
  const [relativeScoreData, setRelativeScoreData] = useState<{ [key: string]: any }[]>([]);
  const [relativeScoreLoading, setRelativeScoreLoading] = useState<boolean>(true);
  const [relativeScoreError, setRelativeScoreError] = useState<boolean>(false);

  const orgUnit: RestOrganizationUnit = useParamSelector(orgUnitGetRestDataSelect, {
    id: ou_id
  });

  const ouProdScoreState = useParamSelector(_ouProdScoreSelect, {
    id: OU_PROD_SCORE_ID
  });

  const orgUnitListState = useParamSelector(orgUnitListDataState, {
    id: OU_DASHBOARD_LIST_ID
  });

  const apiFilters = useMemo(() => {
    const timeRange = dashboardTimeRange?.toUpperCase();
    return {
      page: 0,
      page_size: 100,
      filter: {
        ou_ids: [ou_uuid],
        interval: getInterval(timeRange)
      }
    };
  }, [dashboardTimeRange, ou_uuid]);

  useEffect(() => {
    dispatch(
      OrganizationUnitList(
        {
          page_size: 500
        },
        OU_DASHBOARD_LIST_ID
      )
    );
  }, []);

  useEffect(() => {
    const _loading = get(ouProdScoreState, ["loading"], true);
    const error = get(ouProdScoreState, ["error"], true);
    if (!_loading) {
      if (!error) {
        const data = get(ouProdScoreState, ["data", "records"], []);
        setRelativeScoreData(data);
      } else {
        setRelativeScoreError(true);
      }
      setRelativeScoreLoading(_loading);
    } else {
      if (!relativeScoreLoading) {
        setRelativeScoreLoading(_loading);
      }
    }
  }, [ouProdScoreState]);

  const orgListOptions = useMemo(() => {
    const ouList = get(orgUnitListState, ["data", "records"], []);
    return ouList?.map((org: { [key: string]: string }) => ({
      value: org.ou_id,
      label: org.name
    }));
  }, [orgUnitListState]);

  const relativeScoreChartExtraFilters = useMemo(
    () => (
      <>
        <Icon type="clock-circle" />
        <AntSelectComponent
          className="relative-core-dropdown"
          value={relativeScoreFilters?.agg_interval || "month"}
          options={["quarter", "month"]}
          onChange={(value: string) => onRelativeFilterChange(value, "agg_interval")}
        />
        {orgListOptions.length > 0 && (
          <AntSelectComponent
            className="relative-core-dropdown"
            value={relativeScoreFilters?.ou_id}
            placeholder="Select OU"
            options={orgListOptions}
            onChange={(value: string) => onRelativeFilterChange(value, "ou_id")}
          />
        )}
      </>
    ),
    [relativeScoreFilters, orgListOptions]
  );

  const onRelativeFilterChange = (value: string, type: string) => {
    if (type === "ou_id") {
      setRelativeScoreFilters(filter => ({
        ...filter,
        ou_id: value
      }));
    }
    if (type === "agg_interval") {
      setRelativeScoreFilters(filter => ({
        ...filter,
        agg_interval: value
      }));
    }
  };

  const fetchData = () => {
    dispatch(ouScoreOverview((ou_id || "") as string, apiFilters));
  };

  useEffect(() => {
    apiFiltersRef.current = apiFilters;
    fetchData();
  }, []);

  useEffect(() => {
    if (!isEqual(apiFilters, apiFiltersRef.current)) {
      apiFiltersRef.current = apiFilters;
      fetchData();
    }
  }, [apiFilters]);

  useEffect(() => {
    dispatch(restapiClear("organization_unit_productivity_score", "list", "-1"));
    let filter = {
      report_requests: [
        {
          id_type: "org_ids",
          org_ids: [ou_uuid]
        }
      ],
      agg_interval: relativeScoreFilters?.agg_interval || "month"
    };
    if (relativeScoreFilters?.ou_id) {
      filter = {
        ...filter,
        report_requests: [
          {
            id_type: "org_ids",
            org_ids: [ou_uuid, relativeScoreFilters?.ou_id]
          }
        ]
      };
    }
    dispatch(
      ouProductivityScore(OU_PROD_SCORE_ID, {
        page: 0,
        page_size: 100,
        filter
      })
    );
  }, [relativeScoreFilters, ou_uuid]);

  const onFilterValueChange = useCallback((value: any) => {
    setDashboardTimeRange(value);
  }, []);

  if (!orgUnit.id) {
    return (
      <div className="w-100p h-100p flex justify-center align-center">
        <Spin />
      </div>
    );
  }

  return (
    <div className="dev-productivity-dashboard-container">
      <ScoreCardDashboardHeader
        dashboardTimeRange={dashboardTimeRange}
        onFilterValueChange={onFilterValueChange}
        dashboardTitle={`${orgUnit?.name ?? ""} Overview`}
        ouView={true}
      />
      <div
        className={cx("dev-productivity-widget-list", {
          "px-20": !getIsStandaloneApp()
        })}>
        <OUScoreOverViewComponent />
        <div className="widgets">
          {devProductivityWidgets.map(widget =>
            widget.widget_type === scoreWidgetType.ORGANIZATION_UNIT_PRODUCTIVITY_SCORE ? (
              <RelativeScoreChart
                extraFilters={relativeScoreChartExtraFilters}
                data={relativeScoreData}
                loading={relativeScoreLoading}
                error={relativeScoreError}
              />
            ) : (
              <DevWidgetTimeRangeContext.Provider
                value={{
                  dashboardTimeRange: dashboardTimeRange
                }}>
                <DevWidgetContainer widget={widget} filters={apiFilters} ou_uuid={(ou_uuid ?? "") as string} />
              </DevWidgetTimeRangeContext.Provider>
            )
          )}
        </div>
      </div>
    </div>
  );
};

export default DevProductivityDashboard;
