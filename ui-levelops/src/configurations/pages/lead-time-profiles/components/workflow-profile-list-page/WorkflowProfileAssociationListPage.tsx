import { Empty } from "antd";
import { RestVelocityConfigs } from "classes/RestVelocityConfigs";
import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { modalNameColumn } from "configurations/pages/ticket-categorization/helper/profiles.helper";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import React, { useMemo } from "react";
import { getGenericRestAPIStatusSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  velocityConfigsRestListSelector,
  VELOCITY_CONFIGS,
  VELOCITY_CONFIG_LIST_ID
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { WebRoutes } from "routes/WebRoutes";
import { ProfilesPaginatedTable } from "shared-resources/containers";
import { useHistory } from "react-router-dom";
import { getSelectedIntegrationIds } from "../../helpers/profileIntHelper";

interface WorkflowProfileAssociationListPageProps {
  ouIntegrationIds: number[];
  setProfilesAvailable?: (profilesAvailable: boolean) => void;
}

const WorkflowProfileAssociationListPage: React.FC<WorkflowProfileAssociationListPageProps> = ({
  ouIntegrationIds,
  setProfilesAvailable
}) => {
  const LTFCAndMTTRSupport = useHasEntitlements(Entitlement.LTFC_MTTR_DORA_IMPROVEMENTS, EntitlementCheckType.AND);
  const velocityConfigsListState: Array<RestVelocityConfigs | RestWorkflowProfile> = useParamSelector(
    velocityConfigsRestListSelector,
    {
      id: VELOCITY_CONFIG_LIST_ID
    }
  );
  const configStatus = useParamSelector(getGenericRestAPIStatusSelector, {
    uri: VELOCITY_CONFIGS,
    method: "list",
    uuid: VELOCITY_CONFIG_LIST_ID
  });
  const editUrl = WebRoutes.velocity_profile.scheme.edit;
  const history = useHistory();

  const filteredProfiles = useMemo(() => {
    if (configStatus.loading) return [];
    return velocityConfigsListState.filter((profile: RestVelocityConfigs | RestWorkflowProfile) => {
      if (!profile.is_new) return false;
      const selectedIntegrationIds = getSelectedIntegrationIds(profile as RestWorkflowProfile, LTFCAndMTTRSupport);
      return selectedIntegrationIds.every(int => ouIntegrationIds.includes(int));
    });
  }, [velocityConfigsListState, ouIntegrationIds]);

  if (configStatus.loading === false && !filteredProfiles.length) {
    setProfilesAvailable && setProfilesAvailable(false);
    return <Empty description="There are no profiles that can be associated with the current collection." />;
  }

  setProfilesAvailable && setProfilesAvailable(true);
  return (
    <ProfilesPaginatedTable
      dataSource={filteredProfiles}
      columns={[modalNameColumn(filteredProfiles, editUrl, "Workflow")]}
      showInfo
      isModalView
    />
  );
};

export default WorkflowProfileAssociationListPage;
