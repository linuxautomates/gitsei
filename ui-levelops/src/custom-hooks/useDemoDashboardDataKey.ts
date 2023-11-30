import { RestDashboard } from "classes/RestDashboards";
import { get, isEqual } from "lodash";
import { useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { slugifyId } from "utils/stringUtils";

/** This custom hook will provide us with the demo data key for fetching demo data*/
export const useDemoDashboardDataId = (widgetID: any) => {
  const [demoDataKey, setDemoDataKey] = useState<string | undefined>(undefined);

  const dashboardOUOptionsState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: "dashboard_ou_options"
  });

  const dashboard: RestDashboard = useSelector(selectedDashboard) as RestDashboard;
  const widgetData = useSelector(state =>
    get(state, ["restapiReducer", "selected-dashboard", "metadata", "demo_data", widgetID], {})
  );
  const widgetFilters = useMemo(() => {
    return get(widgetData, "widget_filters", {});
  }, [widgetData]);

  useEffect(() => {
    const loading = get(dashboardOUOptionsState, "loading", true);
    const error = get(dashboardOUOptionsState, "error", true);
    if (!loading && !error) {
      const data = get(dashboardOUOptionsState, "data", []);
      const records = get(data, ["selectedOptions"], []);

      const dashboardEffortInvestmentProfile = get(dashboard, ["metadata", "effort_investment_profile"], undefined);

      if (records?.length) {
        const newRec = records?.map((rec: any) => slugifyId(rec?.name?.toLowerCase() ?? "")).filter((v: any) => !!v);

        if (dashboardEffortInvestmentProfile) {
          const efforTInvestmentProfile = get(dashboard, ["metadata", "effort_investment_profile_filter"], undefined);
          if (efforTInvestmentProfile) {
            newRec.push(efforTInvestmentProfile);
          }
        }
        if (widgetFilters) {
          const filters = Object.values(widgetFilters);
          filters.forEach(filter => {
            newRec.push(filter);
          });
        }
        const demoDashDataKey = newRec.sort().join("##");
        console.log("[Demo Dashboard Data Key]", demoDashDataKey);
        if (!isEqual(demoDashDataKey, demoDataKey)) {
          setDemoDataKey(demoDashDataKey);
        }
      }
    }
  }, [dashboard, widgetFilters, dashboardOUOptionsState]);

  return demoDataKey;
};
