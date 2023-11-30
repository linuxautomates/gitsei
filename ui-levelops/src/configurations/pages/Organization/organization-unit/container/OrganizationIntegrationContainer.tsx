import React, { useEffect, useMemo, useState } from "react";
import { Badge, Divider, Icon } from "antd";
import queryString from "query-string";
import { cloneDeep, get } from "lodash";
import {
  orgUnitBasicInfoType,
  OrgUnitSectionKeys,
  OrgUnitSectionPayloadType
} from "configurations/configuration-types/OUTypes";
import "./OrganizationIntegrationContainer.styles.scss";
import OrganizationIntegrationConfigureComponent from "../OrganizationIntegrationConfigureComponent";
import OrgUnitIntegrationConfigureOldComponent from "../org-filters-old-flow/OrgUnitIntegrationConfigureOldComponent";
import { INTEGRATION_WITH_NEW_FILTER_FLOW } from "../../Constants";
import { getIntegrationOptions } from "./helper";
import { AntButton } from "shared-resources/components";
import { useLocation } from "react-router-dom";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericWorkSpaceUUIDSelector } from "reduxConfigs/selectors/workspace/workspace.selector";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { getIsStandaloneApp } from "helper/helper";
import { useWorkspace } from "custom-hooks/useWorkspace";
import { useAppStore } from "contexts/AppStoreContext";
import { useWorkSpaceList } from "custom-hooks/workspace/useWorkSpaceList";

interface OrganizationUnitIntegrationSectionProps {
  handleRemoveIntegration: (id: string) => void;
  handleAddIntegration: () => void;
  handleOUUpdate: (key: orgUnitBasicInfoType, value: any) => void;
  sectionList: OrgUnitSectionPayloadType[];
}

const OrganizationUnitIntegrationSection: React.FC<OrganizationUnitIntegrationSectionProps> = ({
  handleAddIntegration,
  handleRemoveIntegration,
  handleOUUpdate,
  sectionList
}) => {
  const location = useLocation();
  let { ou_workspace_id } = queryString.parse(location.search);
  const [selectedWorkspace, setSelectedWorkspace] = useState<WorkspaceModel>();
  const workSpaceListState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method: "list",
    uuid: "workspace_list"
  });

  const { selectedProject, accountInfo } = useAppStore();
  const { identifier: accountId = "" } = accountInfo || {};
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};

  const { selectedWorkspace: harnessProject } = useWorkspace({
    accountId,
    orgIdentifier,
    projectIdentifier
  });
  const { workSpaceListData } = useWorkSpaceList();
  const isStandaloneApp = getIsStandaloneApp();

  useEffect(() => {
    if (isStandaloneApp) {
      if (workSpaceListData.length) {
        const currentWorkspace = workSpaceListData.find(
          (workspace: WorkspaceModel) => workspace.id === ou_workspace_id
        );
        if (currentWorkspace) {
          setSelectedWorkspace(currentWorkspace);
        }
      }
    } else {
      setSelectedWorkspace(harnessProject);
    }
  }, [ou_workspace_id, workSpaceListData, harnessProject]);

  const handleOUSectionChanges = (key: OrgUnitSectionKeys, value: any, id: string) => {
    const corrSection = sectionList.find(section => section.id === id);
    if (corrSection) {
      (corrSection as any)[key] = value;
      if (key === "type") {
        (corrSection as any)["integration"] = {};
        (corrSection as any)["user_groups"] = [];
      }
      const index = sectionList.findIndex(section => section.id === id);
      const newSections = cloneDeep(sectionList || []);
      newSections[index] = corrSection;
      handleOUUpdate("sections", newSections);
    }
  };

  const disbaleAddIntegrations = useMemo(() => {
    const alreadySelectedIds = sectionList
      .map((section: any) => {
        const applicationAndId: string[] = (section?.type || "")?.split("@");
        return applicationAndId?.[1];
      })
      .filter((id: any) => !!id);

    const filteredIntegrations = (selectedWorkspace?.integrations ?? []).filter(
      (record: any) => !alreadySelectedIds.includes(record?.id)
    );

    const sectionWithoutType = sectionList?.find((section: any) => !section?.type);

    return filteredIntegrations?.length === 0 || !!sectionWithoutType;
  }, [sectionList, selectedWorkspace]);

  return (
    <div className="ou-integration-section">
      <div className="ou-intgration-title">
        Integrations{" "}
        <span>
          <div className="ml-5">
            <Badge style={{ backgroundColor: "#1EA9DB" }} count={(sectionList || []).length} />
          </div>
        </span>
      </div>
      <Divider />
      {!(sectionList || []).length && <p>INCLUDE: All Existing Integrations</p>}
      <div className="integration-list-wrapper">
        {(sectionList || []).map(section => {
          const integrations = getIntegrationOptions(section, sectionList, selectedWorkspace ?? {});
          return section?.type &&
            INTEGRATION_WITH_NEW_FILTER_FLOW.includes(section?.type.split("@")[0] as IntegrationTypes) ? (
            <OrganizationIntegrationConfigureComponent
              section={section}
              handleOUSectionChanges={handleOUSectionChanges}
              handleRemoveSection={handleRemoveIntegration}
              integrations={integrations}
            />
          ) : (
            <OrgUnitIntegrationConfigureOldComponent
              section={section}
              handleOUSectionChanges={handleOUSectionChanges}
              handleRemoveSection={handleRemoveIntegration}
              integrations={integrations}
            />
          );
        })}
      </div>
      <AntButton className="integration-add-action" disabled={disbaleAddIntegrations} onClick={handleAddIntegration}>
        <Icon type="plus-circle" />
        Add Integration{" "}
      </AntButton>
    </div>
  );
};

export default OrganizationUnitIntegrationSection;
