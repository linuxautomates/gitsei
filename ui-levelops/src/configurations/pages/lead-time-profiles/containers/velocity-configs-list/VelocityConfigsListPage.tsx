import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { notification } from "antd";
import { useDispatch } from "react-redux";
import Loader from "components/Loader/Loader";
import { velocityConfigsList } from "reduxConfigs/actions/restapi/velocityConfigs.actions";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { VELOCITY_CONFIGS, VELOCITY_CONFIG_LIST_ID } from "reduxConfigs/selectors/velocityConfigs.selector";
import { getGenericRestAPIStatusSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { VelocityConfigsPageContent } from "../../components";
import { getBreadcumsForListPage } from "../../components/velocity-config-list/helper/getBreadcumsForListPage";
import WorkflowProfileAssociationListPage from "../../components/workflow-profile-list-page/WorkflowProfileAssociationListPage";

interface VelocityConfigsListPageProps {
  isModalView?: boolean;
  ouIntegrationIds?: number[];
  setProfilesAvailable?: (profileAvailable: boolean) => void;
}

const VelocityConfigsListPage: React.FC<VelocityConfigsListPageProps> = (props: VelocityConfigsListPageProps) => {
  const dispatch = useDispatch();
  const location = useLocation();

  const configStatus = useParamSelector(getGenericRestAPIStatusSelector, {
    uri: VELOCITY_CONFIGS,
    method: "list",
    uuid: VELOCITY_CONFIG_LIST_ID
  });

  useEffect(() => {
    dispatch(velocityConfigsList({}, VELOCITY_CONFIG_LIST_ID));
    if (!props.isModalView) {
      const settings = {
        title: "Workflow Profiles",
        bread_crumbs: getBreadcumsForListPage(),
        bread_crumbs_position: "before",
        withBackButton: true
      };
      dispatch(setPageSettings(location.pathname, settings));
      return () => {
        dispatch(clearPageSettings(location.pathname));
      };
    }
  }, []);

  useEffect(() => {
    const { loading, error } = configStatus;
    if (!loading) {
      if (error !== undefined && error !== false) {
        notification.error({ message: "Failed to Fetch Data. Please try again" });
      }
    }
  }, [configStatus]);

  if (configStatus?.loading !== false) return <Loader />;

  return props.isModalView ? (
    <WorkflowProfileAssociationListPage
      ouIntegrationIds={props.ouIntegrationIds || []}
      setProfilesAvailable={props.setProfilesAvailable}
    />
  ) : (
    <VelocityConfigsPageContent />
  );
};

export default VelocityConfigsListPage;
