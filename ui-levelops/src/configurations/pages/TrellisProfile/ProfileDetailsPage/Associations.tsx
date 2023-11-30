import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import React from "react";
import { AntText } from "shared-resources/components";
import AssociationsOrgSelector from "./associationsDetails/AssociationOrgSelector";
import AssociationInvestmentProfile from "./associationsDetails/AssociationInvestmentProfile";
import AssociationAdvancedOptions from "./associationsDetails/AssociationAdvancedOptions";

interface AssociationsProps {
  profile: RestTrellisScoreProfile;
  handleChanges: (section_name: string, value: any, type: string) => void;
  profilesList: Array<any>;
  ticketCategorizationData: any;
}

const Associations: React.FC<AssociationsProps> = ({
  profile,
  handleChanges,
  profilesList,
  ticketCategorizationData
}) => {
  return (
    <div className="dev-score-profile-container-section">
      <div className="dev-score-profile-container-section-container">
        <div className="dev-score-profile-container-section-container-header">
          <AntText className="section-header">ASSOCIATIONS</AntText>
        </div>
      </div>
      <AssociationsOrgSelector profile={profile} handleChanges={handleChanges} profilesList={profilesList} />
      <AssociationInvestmentProfile
        profile={profile}
        handleChanges={handleChanges}
        ticketCategorizationData={ticketCategorizationData}
      />
      <AssociationAdvancedOptions profile={profile} handleChanges={handleChanges} />
    </div>
  );
};

export default Associations;
