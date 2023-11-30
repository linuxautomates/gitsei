import { Divider, notification } from "antd";
import Loader from "components/Loader/Loader";
import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useLocation } from "react-router-dom";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { trellisProfilesListReadAction } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { TrellisProfilesListState } from "reduxConfigs/reducers/trellisProfileReducer";
import { trellisProfileListSelector } from "reduxConfigs/selectors/trellisProfileSelectors";
import EmptyApiErrorWidgetComponent from "shared-resources/components/empty-api-error-widget/empty-api-error-widget.component";
import { TICKET_CATEGORIZATION_PROFILE_LIST_FAIL_MESSAGE } from "../../ticket-categorization/constants/ticket-categorization.constants";
import { getBreadcrumbsForTrellisPage } from "../../ticket-categorization/helper/getBreadcumsForTrellisPage";
import { TEXT_ERROR_DESC } from "../constant";
import IntegrationMissing from "./IntegrationMissing";
import TrellisScoreProfileContent from "./TrellisScoreProfileContent";

interface TrellisScoreProfileLandingPageProps {
  isModalView?: boolean;
}

const TrellisScoreProfileLandingPage: React.FC<TrellisScoreProfileLandingPageProps> = ({ isModalView }) => {
  const dispatch = useDispatch();
  const location = useLocation();

  const trellisScoreProfilesList: TrellisProfilesListState = useSelector(trellisProfileListSelector);

  useEffect(() => {
    dispatch(trellisProfilesListReadAction());
  }, []);

  useEffect(() => {
    if (!isModalView) {
      const settings = {
        title: "Trellis Score Profiles",
        bread_crumbs: getBreadcrumbsForTrellisPage(),
        bread_crumbs_position: "before",
        withBackButton: true
      };
      dispatch(setPageSettings(location.pathname, settings));
    }
  }, []);

  useEffect(() => {
    const { error } = trellisScoreProfilesList;
    if (error) {
      notification.error({ message: TICKET_CATEGORIZATION_PROFILE_LIST_FAIL_MESSAGE });
    }
  }, [trellisScoreProfilesList]);

  if (trellisScoreProfilesList.isLoading) return <Loader />;
  if (trellisScoreProfilesList.error) {
    return (
      <div>
        <Divider className="divider-spacing" />
        <EmptyApiErrorWidgetComponent
          description={<p className="error-line-1">{TEXT_ERROR_DESC}</p>}
          minHeight="2rem"
        />
      </div>
    );
  }
  if (trellisScoreProfilesList.requireIntegrations) {
    return <IntegrationMissing />;
  }
  return <TrellisScoreProfileContent isModalView={isModalView} />;
};

export default TrellisScoreProfileLandingPage;
