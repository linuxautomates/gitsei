import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import React, { FC, useMemo } from "react";
import { useWorkFlowProfileFilters } from "custom-hooks/Dora/useWorkFlowProfileData";
import { WebRoutes } from "routes/WebRoutes";
import { useSelector } from "react-redux";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  workflowProfileDetailSelector,
  workflowProfileDetailSelectorLoading
} from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { Spin } from "antd";
import { DoraOrgUnitDataViewProps } from "dashboard/graph-filters/components/GenericFilterComponents/DoraGenericFilter/DoraOrgUnitDataView";
import { getURL } from "./helper";

type ProfileApiContainerProps = {
  filterProps: {
    allFilters: any;
    renderComponent: FC<DoraOrgUnitDataViewProps>;
    label: string;
    workflowData: RestWorkflowProfile;
  };
  report: string;
};

const DoraProfileApiContainer: FC<ProfileApiContainerProps> = props => {
  let { report, filterProps } = props;
  const selectedOUState = useSelector(getSelectedOU);
  const workflowProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState.id });
  const workFlowProfileLoading = useParamSelector(workflowProfileDetailSelectorLoading, {
    queryParamOU: selectedOUState.id
  });

  const { data, loading, count } = useWorkFlowProfileFilters(report);

  const URL = WebRoutes.velocity_profile.scheme.editTab(getURL(report, workflowProfile?.id));

  const renderComp = useMemo(() => {
    if (loading || workFlowProfileLoading) {
      return (
        <div className="dora-filter-view-wrapper">
          <Spin />
        </div>
      );
    }

    if (!data || !workflowProfile) {
      return null;
    }
    return React.createElement(filterProps.renderComponent, {
      data: data,
      count: count,
      URL: URL,
      filterProps: {
        ...filterProps
      }
    });
  }, [data, props]);

  return renderComp;
};

export default DoraProfileApiContainer;
