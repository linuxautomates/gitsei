import React, { useEffect, useMemo, useState } from "react";
import { DEV_PROD_PR_ACTIVITY_ID, REQUIRE_TRELLIS_TEXT } from "dashboard/pages/scorecard/constants";
import {
  devProdPRActivityData,
  devProdPRActivityError,
  devProdPRActivityLoading
} from "reduxConfigs/selectors/devProductivity.selector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import ActivityTable from "shared-resources/charts/activity-table/ActivityTable";
import { AntCard, AntText } from "shared-resources/components";
import { Spin } from "antd";
import { trellisPRActivityAction } from "reduxConfigs/actions/restapi/trellisReportActions";
import { transformActivityData } from "./helpers";
import "./pr-activity.scss";
import { useDispatch } from "react-redux";
import PRActivityTitle from "./PRActivityTitle";
import { getPRActivityColumns } from "./prActivity.tableConfig";

interface PRActivityComponentProps {
  dashboardTimeGtValue: string;
  dashboardTimeLtValue: string;
  userIdType?: string | string[];
  user_id?: string | string[] | null;
  onViewBySelectorChange?(): void;
  trellis_profile: any;
  profileKey: Record<any, any> | undefined;
}

const PRActivityComponent = (props: PRActivityComponentProps) => {
  const [selectedTimeRange, setSelectedTimeRange] = useState<{ $gt: string; $lt: string }>();
  const [columns, setColumns] = useState<Array<any>>([]);
  const dispatch = useDispatch();

  const { userIdType, user_id, trellis_profile, profileKey } = props;

  const devProdPRActivityLoadingState = useParamSelector(devProdPRActivityLoading, { id: DEV_PROD_PR_ACTIVITY_ID });
  const devProdPRActivityErrorState = useParamSelector(devProdPRActivityError, { id: DEV_PROD_PR_ACTIVITY_ID });
  const prActicityData = useParamSelector(devProdPRActivityData, { id: DEV_PROD_PR_ACTIVITY_ID });
  useEffect(() => {
    if (trellis_profile) {
      dispatch(trellisPRActivityAction(userIdType, user_id, selectedTimeRange, trellis_profile.id, profileKey));
    }
  }, [selectedTimeRange, userIdType, user_id, profileKey?.ou_ref_ids]);

  useEffect(() => {
    setColumns(getPRActivityColumns(selectedTimeRange));
  }, [selectedTimeRange]);

  const data = useMemo(() => {
    if (devProdPRActivityLoadingState || devProdPRActivityErrorState) {
      return undefined;
    }
    return transformActivityData(prActicityData.records[0]);
  }, [prActicityData, devProdPRActivityLoadingState]);

  return (
    <AntCard
      className="pr-activity-container"
      title={
        <PRActivityTitle
          title={"PR Activity"}
          dashboardTimeGtValue={props.dashboardTimeGtValue}
          dashboardTimeLtValue={props.dashboardTimeLtValue}
          setSelectedTimeRange={setSelectedTimeRange}
        />
      }>
      {!trellis_profile ? (
        <div className="dev-prod-relative-score-container-spinner">
          <AntText>{REQUIRE_TRELLIS_TEXT}</AntText>
        </div>
      ) : devProdPRActivityLoadingState ? (
        <div className="w-100p h-100p flex justify-center align-center">
          <Spin />
        </div>
      ) : (
        <ActivityTable data={data} columns={columns} />
      )}
    </AntCard>
  );
};

export default PRActivityComponent;
