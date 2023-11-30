import React, { useEffect } from "react";
import { RouteComponentProps } from "react-router-dom";
import { useDispatch } from "react-redux";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import OrgUserListPage from "../User/container/UserListPage/UserListPage";
import "./OrganizationUsersListContainer.scss";

interface OrganizationUsersListContainerProps extends RouteComponentProps {}

const OrganizationUsersListContainer: React.FC<OrganizationUsersListContainerProps> = props => {
  const dispatch = useDispatch();

  useEffect(() => {
    return () => {
      dispatch(clearPageSettings(props.location.pathname));
    };
  }, []);

  return <OrgUserListPage />;
};
export default OrganizationUsersListContainer;
