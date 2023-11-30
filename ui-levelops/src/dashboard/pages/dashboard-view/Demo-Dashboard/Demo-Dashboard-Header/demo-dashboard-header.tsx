import React, { useCallback, useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { newDashboardUpdate } from "reduxConfigs/actions/restapi";
import { useHistory } from "react-router-dom";
import "./demo-dashboard-header.style.scss";
import queryString from "query-string";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { DemoEffortInvestmentProfileValues, DEMO_EFFORT_INVESTMENT_PROFILE_OPTIONS } from "./constant";
import DemoDashboardHeaderSearchComponent from "./DemoDashboardHeaderSearchComponent";
import DemoDashboardDatePicker from "../DemoDashboardDatePicker/DemoDashboardDatePicker";
import DashboardCustomOU from "dashboard/components/dashboard-header/DashboardCustomOU/dashboard-custom-ou-wrapper";
import { AntSelect, AntText, AntRow } from "shared-resources/components";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { get } from "lodash";
import "./demo-dashboard-header.style.scss";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { getOUOptionsAction } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
interface DashboardHeaderProps {
  dashboardId?: any;
  queryparamOU?: string | undefined;
}

const DemoDashboardHeader: React.FC<DashboardHeaderProps> = ({ queryparamOU, dashboardId }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const params = queryString.parse(history.location.search) as any;
  const OU = queryString.parse(history.location.search)?.OU as string;
  const dashboard = useSelector(selectedDashboard);
  const selectedOUState = useSelector(getSelectedOU);

  useEffect(() => {
    dispatch(setPageSettings(history.location.pathname, { search: history.location.search }));
  }, []);

  const handleProfileChange = (value: DemoEffortInvestmentProfileValues, key: string) => {
    const form = {
      metadata: {
        ...dashboard?._metadata,
        [key]: value
      }
    };
    dispatch(newDashboardUpdate(dashboard?.id, form));
  };

  const getOUId = useCallback(() => {
    return selectedOUState?.ou_id;
  }, [selectedOUState]);

  useEffect(() => {
    dispatch(getOUOptionsAction("organization_unit_management", "list", "dashboard_ou_options"));
  }, [OU]);

  const demoEffortInvestmentProfile = useMemo(() => {
    return get(dashboard, ["metadata", "effort_investment_profile"], false);
  }, []);
  return (
    <div>
      <AntRow>{queryparamOU && <DashboardCustomOU dashboardId={dashboardId} isDemo={true}></DashboardCustomOU>}</AntRow>
      <div className="demo-dashboard-view-secondary-header">
        <div className="demo-dashboard-search-div">
          <DemoDashboardHeaderSearchComponent ouId={getOUId()} />
        </div>
        <div className="mr-20">
          <DemoDashboardDatePicker />
        </div>
      </div>
      <div className="demo-dashboard-view-secondary-header">
        <div className="demo-profile-dashboard-time-div">
          {demoEffortInvestmentProfile && (
            <div className="dashboard-view-secondary-header-right flex justify-end">
              <div className="flex justify-center align-baseline">
                {demoEffortInvestmentProfile && (
                  <AntText color="#595959" className="view-text-style ml-15">
                    Alignment Profile :
                  </AntText>
                )}
                {demoEffortInvestmentProfile && (
                  <div className="effort-investment-profile-container">
                    <AntSelect
                      options={DEMO_EFFORT_INVESTMENT_PROFILE_OPTIONS}
                      value={get(
                        dashboard?.metadata ?? {},
                        ["effort_investment_profile_filter"],
                        DemoEffortInvestmentProfileValues.INITIATIVES
                      )}
                      onChange={(value: DemoEffortInvestmentProfileValues) =>
                        handleProfileChange(value, "effort_investment_profile_filter")
                      }
                    />
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DemoDashboardHeader;
