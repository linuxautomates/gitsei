import React from "react";
import { useParams, useLocation } from "react-router-dom";
import CreateProfileContainer from "../create-edit-profile/CreateProfileContainer";
import EditProfileContainer from "../create-edit-profile/EditProfileContainer";
import queryString from "query-string";
import { NEW_SCHEME_ID } from "../../constants/ticket-categorization.constants";
import { QueryStringType } from "../../types/ticketCategorization.types";

const ProfileContainer: React.FC = () => {
  const params = useParams();
  const location = useLocation();
  const edit: QueryStringType = queryString.parse(location.search).edit;
  const profileId = (params as any).id;

  const isNew = profileId === NEW_SCHEME_ID;

  return <>{isNew ? <CreateProfileContainer /> : <EditProfileContainer id={profileId} edit={edit} />}</>;
};

export default ProfileContainer;
