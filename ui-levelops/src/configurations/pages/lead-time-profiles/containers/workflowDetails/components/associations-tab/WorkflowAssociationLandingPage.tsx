import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import React from "react";
import { AntText } from "shared-resources/components";
import WorkflowAssociationsOrgSelector from "./WorkflowAssociationOrgSelector";

interface WorkflowAssociationsLandingPageProps {
  profile: RestWorkflowProfile;
  onChange: (newValue: any) => void;
  profilesList: Array<any>;
  setExclamationFlag: (value: boolean) => void;
}

const WorkflowAssociationsLandingPage: React.FC<WorkflowAssociationsLandingPageProps> = ({
  profile,
  onChange,
  profilesList,
  setExclamationFlag
}) => {
  const handleChangesAssociatioon = (value: any, type: string) => {
    onChange({
      [type]: value
    });
  };

  return (
    <div className="profile-basic-info-container dev-score-profile-container-section">
      <div className="dev-score-profile-container-section-container-header">
        <AntText className="section-header">ASSOCIATIONS</AntText>
      </div>
      <WorkflowAssociationsOrgSelector
        profile={profile}
        handleChanges={handleChangesAssociatioon}
        profilesList={profilesList}
        setExclamationFlag={setExclamationFlag}
      />
    </div>
  );
};

export default WorkflowAssociationsLandingPage;
