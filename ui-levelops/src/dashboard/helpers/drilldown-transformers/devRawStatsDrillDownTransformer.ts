import {
  getDashboardTimeGtValue,
  getDashboardTimeLtValue
} from "dashboard/components/dashboard-view-page-secondary-header/helper";

export const devRawStatsDrilldownTransformer = (drillDownData: any) => {
  const { drillDownProps } = drillDownData;
  const { dev_raw_stats } = drillDownProps;
  const { record, columnName, interval, dashboardOuIds } = dev_raw_stats;
  let user_id_list = [];
  user_id_list.push(record?.org_user_id);

  const intervalGtValue = getDashboardTimeGtValue(interval);
  const intervalLtValue = getDashboardTimeLtValue(interval);

  return {
    filters: {
      filter: {
        ou_ref_ids: dashboardOuIds,
        feature_name: columnName,
        user_id_type: "ou_user_ids",
        user_id_list: user_id_list,
        time_range: {
          $gt: intervalGtValue,
          $lt: intervalLtValue
        }
      }
    }
  };
};
