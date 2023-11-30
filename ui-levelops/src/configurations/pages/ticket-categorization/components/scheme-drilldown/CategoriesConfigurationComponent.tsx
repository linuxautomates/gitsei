import React from "react";
import CategoriesConfigurationPageContent from "./CategoriesConfigurationPageContent";
import { ProfileUpdateContext } from "dashboard/pages/context";
import { RestTicketCategorizationProfileJSONType } from "../../types/ticketCategorization.types";

const CategoriesConfigurationComponent: React.FC<{
  profileId: string;
  handleUpdate: (updatedScheme: RestTicketCategorizationProfileJSONType) => void;
}> = ({ profileId, handleUpdate }) => {
  return (
    <ProfileUpdateContext.Provider value={{ handleUpdate }}>
      <CategoriesConfigurationPageContent profileId={profileId} />
    </ProfileUpdateContext.Provider>
  );
};

export default CategoriesConfigurationComponent;
