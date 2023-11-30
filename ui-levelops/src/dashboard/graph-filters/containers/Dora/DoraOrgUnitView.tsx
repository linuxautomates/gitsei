import React, { FC, useMemo } from "react";
import { useParams } from "react-router-dom";
import { useOrgUnitData } from "custom-hooks/Dora/useOrgUnitData";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { transformOrgIntegrations } from "./helper";
import { useSelector } from "react-redux";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { WebRoutes } from "routes/WebRoutes";
import { Spin } from "antd";
import { ProjectPathProps } from "classes/routeInterface";
type OrgFilterInfoComponentProps = {
  filterProps: LevelOpsFilter;
};

const OrgUnitFilterViewApiContainer: FC<OrgFilterInfoComponentProps> = props => {
  const { data, loading } = useOrgUnitData();
  const selectedOUState = useSelector(getSelectedOU);
  const projectParams = useParams<ProjectPathProps>();
  const URL = WebRoutes.organization_page.edit(
    projectParams,
    selectedOUState.id,
    selectedOUState.workspace_id,
    selectedOUState.ou_category_id
  );
  const transformedData = useMemo(() => {
    return transformOrgIntegrations(data);
  }, [data]);
  const renderFilter = useMemo(() => {
    if (loading) {
      return (
        <div className="dora-filter-view-wrapper">
          <Spin />
        </div>
      );
    }
    return React.createElement(props.filterProps.renderComponent, {
      ...props,
      data: transformedData,
      URL: URL,
      count: transformedData?.length - 2,
      filterProps: {
        ...props.filterProps
      }
    });
  }, [props, data]);

  return renderFilter;
};

export default OrgUnitFilterViewApiContainer;
