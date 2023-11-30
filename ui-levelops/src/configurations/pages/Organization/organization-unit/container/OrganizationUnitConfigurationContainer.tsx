import React, { useCallback } from "react";
import { v1 as uuid } from "uuid";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import {
  orgUnitBasicInfoType,
  OrgUnitSectionKeys,
  OrgUnitSectionPayloadType
} from "configurations/configuration-types/OUTypes";
import OrganizationUnitUserSection from "../OrganizationUnitUserSection";
import OrganizationUnitIntegrationSection from "./OrganizationIntegrationContainer";
import { Typography } from "antd";

interface OrgUnitConfigurationProps {
  draftOrgUnit: RestOrganizationUnit;
  handleOrgUnitUpdate: (key: orgUnitBasicInfoType, value: any) => void;
}

const OrganizationUnitConfigurationContainer: React.FC<OrgUnitConfigurationProps> = ({
  handleOrgUnitUpdate,
  draftOrgUnit
}) => {
  const handleAddIntegration = () => {
    const newSection: OrgUnitSectionPayloadType = {
      id: uuid(),
      integration: { id: "", filters: [] },
      type: "",
      type_id: "",
      user_groups: []
    };
    let newSections = [...(draftOrgUnit?.sections || []), newSection];
    handleOrgUnitUpdate("sections", newSections);
  };

  const handleRemoveIntegration = (id: string) => {
    let newSections = (draftOrgUnit?.sections || []).filter(integration => integration?.id !== id);
    handleOrgUnitUpdate("sections", newSections);
  };

  const handleUpdateDefaultSection = useCallback(
    (key: OrgUnitSectionKeys, value: any) => {
      (draftOrgUnit as any)[key] = value;
      handleOrgUnitUpdate("default_section", draftOrgUnit.default_section);
    },
    [draftOrgUnit]
  );

  return (
    <div className="org-unit-configuration-container">
      <Typography.Title level={4}>DEFINITIONS</Typography.Title>
      <div className="content">
        <OrganizationUnitIntegrationSection
          handleAddIntegration={handleAddIntegration}
          handleOUUpdate={handleOrgUnitUpdate}
          handleRemoveIntegration={handleRemoveIntegration}
          sectionList={draftOrgUnit?.sections || []}
        />
        <OrganizationUnitUserSection
          fromIntegration={false}
          csv_users={draftOrgUnit?.csv_users || {}}
          users={draftOrgUnit?.users || []}
          dynamic_user_definitions={draftOrgUnit?.dynamic_user_definition || []}
          handleUpdateParent={handleUpdateDefaultSection}
        />
      </div>
    </div>
  );
};

export default OrganizationUnitConfigurationContainer;
