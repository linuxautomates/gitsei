import React, { useCallback, useEffect, useState, useRef, useMemo } from "react";
import { RouteComponentProps, useParams, useLocation, useHistory } from "react-router-dom";
import { useDispatch } from "react-redux";
import { get, isEqual } from "lodash";
import cx from "classnames";
import queryParser from "query-string";
import ScoreCardDashboardHeader from "dashboard/components/dashboard-view-page-header/ScoreCardDashboardHeader";
import EngineerProfileCard from "../components/EngineerProfileCard";
import {
  trellisRelativeScore,
  trellisReport,
  trellisUserDetails
} from "reduxConfigs/actions/restapi/trellisReportActions";
import { DEV_PROD_ID, DEV_PROD_RELATIVE_SCORE_ID, DEV_PROD_USER_SNAPSHOT_ID, REQUIRE_TRELLIS_TEXT } from "../constants";
import {
  getDashboardTimeGtValue,
  getDashboardTimeLtValue
} from "dashboard/components/dashboard-view-page-secondary-header/helper";
import "./scoreCardDashboardView.styles.scss";
import EngineerScoreOverViewCard from "../components/EngineerScoreOverViewCard";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  _devProdRelativeScoreSelect,
  devProdEngineerSnapshotSelect,
  devProductivityEngineerSnapshotLoading,
  devProductivityReportError,
  devProRestEngineerSelect
} from "reduxConfigs/selectors/devProductivity.selector";
import { Icon, Spin } from "antd";
import { engineerFeatureResponsesType } from "../../../dashboard-types/engineerScoreCard.types";
import FeatureDrillDownComponent from "../components/FeatureDrillDownComponent";
import { WebRoutes } from "../../../../routes/WebRoutes";
import { RestDevProdEngineer } from "../../../../classes/RestDevProdEngineer";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import RelativeScoreChart from "../components/RelativeScoreChart";
import { AntSelectComponent } from "../../../../shared-resources/components/ant-select/ant-select.component";
import PRActivityComponent from "../components/PRActivity/PRActivityComponent";

import { timeInterval } from "../../../constants/devProductivity.constant";
import { ScorecardDashboardTimeRangeOptions } from "dashboard/components/dashboard-view-page-secondary-header/constants";
import { strIsEqual } from "utils/stringUtils";
import { getInterval } from "dashboard/graph-filters/components/DevProductivityFilters/constants";
import { ProjectPathProps } from "classes/routeInterface";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { AssociatedOU, Org } from "../types";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";
import { getIsStandaloneApp } from "helper/helper";

const ScoreCardDashboardViewPage: React.FC<RouteComponentProps> = () => {
  const dispatch = useDispatch();
  const location = useLocation();
  const history = useHistory();
  const { user_id, user_id_type, interval, OU, ou_id } = queryParser.parse(location.search);
  let isValidInterval = Object.values(timeInterval).includes(
    (typeof interval === "string" ? interval?.toUpperCase() : interval?.[0]?.toUpperCase()) as unknown as timeInterval
  );
  const [dashboardTimeRange, setDashboardTimeRange] = useState<any>(
    isValidInterval
      ? typeof interval === "string"
        ? interval?.toLowerCase()
        : interval?.[0]?.toLowerCase()
      : timeInterval.LAST_QUARTER.toLowerCase()
  );
  const [selectedFeature, setSelectedFeature] = useState<engineerFeatureResponsesType | null>(null);
  const [relativeScoreFilters, setRelativeScoreFilters] = useState<{ [key: string]: string }>({});
  const [relativeScoreData, setRelativeScoreData] = useState<{ [key: string]: any }[]>([]);
  const [relativeScoreLoading, setRelativeScoreLoading] = useState<boolean>(true);
  const [relativeScoreError, setRelativeScoreError] = useState<boolean>(false);
  const newTrellisProfile = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);
  const {
    utils: { getLocationPathName }
  } = useParentProvider();

  const userIdRef = useRef<any>();
  const timeRangeRef = useRef<any>();
  const projectParams = useParams<ProjectPathProps>();

  const [pageSize, setPageSize] = useState<number>(10);
  const [page, setPage] = useState<number>(1);
  const [selectedTrellisProfile, setSelectedTrellisProfile] = useState<any>();

  const devRelativeScoreState = useParamSelector(_devProdRelativeScoreSelect, {
    id: DEV_PROD_RELATIVE_SCORE_ID
  });

  const devReportErrorState = useParamSelector(devProductivityReportError, {
    id: DEV_PROD_ID
  });

  const devReportSnapshotLoadingState = useParamSelector(devProductivityEngineerSnapshotLoading, {
    id: DEV_PROD_USER_SNAPSHOT_ID
  });

  const devProdEngineerSnapshotState = useParamSelector(devProdEngineerSnapshotSelect, {
    id: DEV_PROD_USER_SNAPSHOT_ID
  });

  const engineer: RestDevProdEngineer = useParamSelector(devProRestEngineerSelect, {
    id: DEV_PROD_ID
  });

  const dashboardTimeGtValue = useMemo(
    () => (engineer?.start_time ? engineer?.start_time : getDashboardTimeGtValue(dashboardTimeRange)),
    [engineer, dashboardTimeRange]
  );

  const dashboardTimeLtValue = useMemo(
    () => (engineer?.end_time ? engineer?.end_time : getDashboardTimeLtValue(dashboardTimeRange)),
    [engineer, dashboardTimeRange]
  );

  const orgListOptions = useMemo(() => {
    return selectedTrellisProfile?.associated_ous?.length
      ? selectedTrellisProfile.associated_ous.map((org: { [key: string]: string }) => ({
          value: newTrellisProfile ? org?.ou_ref_id?.toString() : org.ou_id,
          label: org.ou_name
        }))
      : [];
  }, [selectedTrellisProfile, newTrellisProfile]);

  const orgListNewOptions = useMemo(() => {
    let ouList = [];
    if (devProdEngineerSnapshotState?.dev_productivity_profiles && newTrellisProfile) {
      ouList = (devProdEngineerSnapshotState?.dev_productivity_profiles || [])?.reduce(
        (acc: Array<Record<string, string>> = [], profile: AssociatedOU) => {
          const ous = (profile?.associated_ous || [])?.map((org: Org) => ({
            value: org?.ou_ref_id?.toString(),
            label: org.ou_name
          }));
          acc.push(...ous);
          return acc;
        },
        []
      );
    }
    return ouList;
  }, [devProdEngineerSnapshotState, newTrellisProfile]);

  useEffect(() => {
    dispatch(
      trellisUserDetails(DEV_PROD_USER_SNAPSHOT_ID, {
        user_id_type: user_id_type ?? "ou_user_ids",
        user_id_list: [user_id]
      })
    );
  }, [user_id, OU]);
  const profileKey = useMemo(() => {
    return newTrellisProfile ? { ou_ref_ids: [OU] } : { dev_productivity_profile_id: selectedTrellisProfile?.id };
  }, [selectedTrellisProfile, newTrellisProfile, OU]);

  const fetchData = () => {
    if (selectedTrellisProfile) {
      const timeRange = dashboardTimeRange?.toUpperCase();
      dispatch(
        trellisReport(DEV_PROD_ID, {
          page: 0,
          page_size: 100,
          filter: {
            user_id_type: user_id_type ?? "ou_user_ids",
            user_id: user_id,
            interval: getInterval(timeRange),
            ...profileKey
          }
        })
      );
    }
  };

  useEffect(() => {
    userIdRef.current = user_id;
    timeRangeRef.current = dashboardTimeRange;
    fetchData();
  }, [user_id, selectedTrellisProfile]);

  useEffect(() => {
    if (selectedTrellisProfile) {
      dispatch(restapiClear("dev_productivity_relative_score", "list", "-1"));
      // this is not in fetch data coz doesnot depend on time range of dashboard and no need to call it on time range change
      let filter = {
        report_requests: [
          {
            id_type: user_id_type ?? "ou_user_ids",
            id: user_id
          } as object
        ],
        agg_interval: relativeScoreFilters?.agg_interval || "month",
        ...profileKey
      };
      if (relativeScoreFilters?.ou_id) {
        filter = {
          ...filter,
          report_requests: [
            ...filter.report_requests,
            {
              id_type: "org_ids",
              org_ids: [relativeScoreFilters?.ou_id]
            }
          ]
        };
      }
      dispatch(
        trellisRelativeScore(DEV_PROD_RELATIVE_SCORE_ID, {
          page: 0,
          page_size: 100,
          filter
        })
      );
    }
  }, [relativeScoreFilters, selectedTrellisProfile, OU]);

  useEffect(() => {
    if (!isEqual(user_id, userIdRef.current) || !isEqual(dashboardTimeRange, timeRangeRef.current)) {
      userIdRef.current = user_id;
      timeRangeRef.current = dashboardTimeRange;
      dispatch(restapiClear("dev_productivity_reports", "list", "-1"));
      fetchData();
    }
  }, [user_id, dashboardTimeRange, selectedTrellisProfile, OU]);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("dev_productivity_reports", "list", DEV_PROD_ID));
      dispatch(restapiClear("dev_productivity_relative_score", "list", "-1"));
    };
  }, []);

  useEffect(() => {
    const _loading = get(devRelativeScoreState, ["loading"], true);
    const error = get(devRelativeScoreState, ["error"], true);
    if (!_loading) {
      if (!error) {
        const data = get(devRelativeScoreState, ["data", "records"], []);
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
  }, [devRelativeScoreState]);

  const onFilterValueChange = useCallback(
    (value: any) => {
      const feature: any = selectedFeature;
      const valueKey = value?.toUpperCase();
      const replaceKey = valueKey.includes(timeInterval.LAST_WEEK)
        ? "in one week"
        : valueKey.includes(timeInterval.LAST_TWO_WEEKS)
        ? "in two weeks"
        : "per month";
      if (feature) {
        feature.name = feature?.name?.replaceAll(/(per month|in one week|in two weeks)/g, replaceKey);
        setSelectedFeature(feature);
      }
      setDashboardTimeRange(value);
    },
    [selectedFeature]
  );

  const onOpenReports = useCallback(() => {
    const url = WebRoutes.dashboard.scorecardDrillDown(
      projectParams,
      user_id as string,
      user_id_type as string,
      selectedFeature?.name || "",
      dashboardTimeGtValue,
      dashboardTimeLtValue,
      selectedTrellisProfile.id,
      dashboardTimeRange?.toUpperCase()
    );
    window.open(`${removeLastSlash(getLocationPathName?.())}${url}`);
  }, [
    user_id,
    user_id_type,
    selectedFeature,
    dashboardTimeGtValue,
    dashboardTimeLtValue,
    interval,
    dashboardTimeRange
  ]);

  const handlePageSizeChange = useCallback(
    (page_size: number) => {
      setPage(1);
      setPageSize(page_size);
    },
    [pageSize, setPageSize, setPage]
  );

  const onFeatureClick = useCallback((feature: engineerFeatureResponsesType) => {
    setPage(1);
    setSelectedFeature(feature);
  }, []);

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

  const extras = useMemo(() => {
    const org = relativeScoreData[0]?.report.find((item: any) => item?.report?.org_id)?.report?.org_id;
    return (
      <>
        <Icon type="clock-circle" />
        <AntSelectComponent
          className="relative-core-dropdown"
          value={relativeScoreFilters?.agg_interval || "month"}
          options={["quarter", "month"]}
          onChange={(value: string) => onRelativeFilterChange(value, "agg_interval")}
        />
      </>
    );
  }, [relativeScoreFilters, relativeScoreData, orgListOptions, OU]);

  const selectedTimeRange = ScorecardDashboardTimeRangeOptions.find(item =>
    strIsEqual(item.key, dashboardTimeRange)
  )?.label;

  const handleOuChange = (selectedOu: string) => {
    const searchParams = location.search;
    history.push(searchParams?.replace(`OU=${OU}`, `OU=${selectedOu}`));
  };
  return (
    <div className="scorecard-container">
      <ScoreCardDashboardHeader
        dashboardTitle="Developer Overview"
        dashboardTimeRange={dashboardTimeRange}
        onFilterValueChange={onFilterValueChange}
        orgListOptions={orgListNewOptions}
        currentOu={OU}
        handleOuChange={handleOuChange}
        newTrellisProfile={newTrellisProfile}
        ouView={false}
      />
      {devReportSnapshotLoadingState ? (
        <div className="scorecard-container-spinner">
          <Spin />
        </div>
      ) : (
        <>
          <div
            className={cx(
              "scorecard-container-layout",
              { "px-20": !getIsStandaloneApp() },
              { "new-trellis-view": newTrellisProfile }
            )}>
            <EngineerProfileCard
              setSelectedTrellisProfile={setSelectedTrellisProfile}
              newTrellisProfile={newTrellisProfile}
            />
            {!devReportErrorState && !!Object.keys(engineer).length && (
              <div className="scorecard-container-layout-body">
                <PRActivityComponent
                  dashboardTimeGtValue={dashboardTimeGtValue}
                  dashboardTimeLtValue={dashboardTimeLtValue}
                  userIdType={user_id_type || "ou_user_ids"}
                  user_id={user_id}
                  trellis_profile={selectedTrellisProfile}
                  profileKey={newTrellisProfile ? { ou_ref_ids: OU } : undefined}
                />
                <RelativeScoreChart
                  extraFilters={extras}
                  data={relativeScoreData}
                  loading={relativeScoreLoading}
                  error={relativeScoreError}
                  displayMessage={selectedTrellisProfile ? undefined : REQUIRE_TRELLIS_TEXT}
                />
                <EngineerScoreOverViewCard
                  selectedFeature={selectedFeature}
                  setSelectedFeature={onFeatureClick}
                  trellis_profile={selectedTrellisProfile}
                />
                {selectedFeature && (
                  <FeatureDrillDownComponent
                    selectedFeature={selectedFeature?.name || ""}
                    setSelectedFeature={setSelectedFeature}
                    user_id={user_id as string}
                    user_id_type={user_id_type as string}
                    dashboardTimeGtValue={dashboardTimeGtValue}
                    dashboardTimeLtValue={dashboardTimeLtValue}
                    dashboardTimeRange={selectedTimeRange}
                    pageSize={pageSize}
                    onOpenReports={onOpenReports}
                    onPageSizeChange={handlePageSizeChange}
                    page={page}
                    setPage={setPage}
                    trellis_profile_id={selectedTrellisProfile.id}
                    interval={dashboardTimeRange?.toUpperCase()}
                    profileKey={profileKey}
                  />
                )}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default ScoreCardDashboardViewPage;
