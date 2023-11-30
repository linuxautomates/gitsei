import React, { useCallback, useState } from "react";
import { useLocation } from "react-router-dom";
import queryParser from "query-string";
import FeatureDrillDownComponent from "../components/FeatureDrillDownComponent";

const ScoreCardDashboardDrilldown: React.FC = () => {
  const location = useLocation();
  const {
    user_id,
    user_id_type,
    selected_feature,
    dashboard_time_gt_value,
    dashboard_time_lt_value,
    trellis_profile_id,
    interval
  } = queryParser.parse(location.search);
  const [pageSize, setPageSize] = useState<number>(50);
  const handlePageSizeChange = useCallback(
    (pageSize: number) => {
      setPage(1);
      setPageSize(pageSize);
    },
    [pageSize, setPageSize]
  );
  const [page, setPage] = useState<number>(1);
  return (
    <div className="score-card-drilldown-container">
      <FeatureDrillDownComponent
        selectedFeature={(selected_feature as string) || ""}
        user_id={user_id as string}
        user_id_type={user_id_type as string}
        dashboardTimeGtValue={dashboard_time_gt_value as string}
        dashboardTimeLtValue={dashboard_time_lt_value as string}
        pageSize={pageSize}
        onPageSizeChange={handlePageSizeChange}
        page={page}
        setPage={setPage}
        trellis_profile_id={trellis_profile_id as string}
        interval={(interval as string) || ""}
      />
    </div>
  );
};

export default ScoreCardDashboardDrilldown;
