import React, { useEffect } from "react";
import { useDispatch } from "react-redux";
import { notification } from "antd";
import { getGenericRestAPIStatusSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { ticketCategorizationSchemesList } from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import Loader from "components/Loader/Loader";
import { ProfileLandingPageContent } from "../../components";
import "./ticketCategorizationProfilePage.styles.scss";
import { ProfileStatusType } from "../../types/ticketCategorization.types";
import {
  TICKET_CATEGORIZATION_PROFILE_LIST_FAIL_MESSAGE,
  TICKET_CATEGORIZATION_SCHEME,
  TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID
} from "../../constants/ticket-categorization.constants";
import { getBreadcumsForListPage } from "../../helper/getBreadcumsForListPage";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { useLocation } from "react-router-dom";

const TicketCategorizationProfilesLandingPage: React.FC = () => {
  const dispatch = useDispatch();
  const location = useLocation();

  const profileStatus: ProfileStatusType = useParamSelector(getGenericRestAPIStatusSelector, {
    uri: "ticket_categorization_scheme",
    method: "list",
    uuid: TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID
  });

  useEffect(() => {
    const settings = {
      title: "Effort Investment Profiles",
      bread_crumbs: getBreadcumsForListPage(),
      bread_crumbs_position: "before",
      withBackButton: true
    };
    dispatch(setPageSettings(location.pathname, settings));
  }, []);

  useEffect(() => {
    dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "create", "-1"));
    dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "get", "-1"));
    dispatch(ticketCategorizationSchemesList({}, TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID));
  }, []);

  useEffect(() => {
    const { loading, error } = profileStatus;
    if (!loading) {
      if (error !== undefined && error !== false) {
        notification.error({ message: TICKET_CATEGORIZATION_PROFILE_LIST_FAIL_MESSAGE });
      }
    }
  }, [profileStatus]);

  if (profileStatus.loading) return <Loader />;

  return <ProfileLandingPageContent />;
};

export default TicketCategorizationProfilesLandingPage;
