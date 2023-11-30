import React from "react";
import { Empty } from "antd";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import {
  NEW_SCHEME_ID,
  UNCATEGORIZED_ID_SUFFIX
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";

import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  ticketCategorizationSchemesRestCreateSelector,
  ticketCategorizationSchemesRestGetSelector
} from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";

import CategoryRankComponent from "../../create-edit-categories/CategoryRankComponent";
import { CategoriesConfigurationPageContentProps } from "../CategoriesConfigurationPageContent";

const CategoriesConfigurationPageContentBody: React.FC<CategoriesConfigurationPageContentProps> = ({ profileId }) => {
  const profile: RestTicketCategorizationScheme = useParamSelector(
    profileId === NEW_SCHEME_ID
      ? ticketCategorizationSchemesRestCreateSelector
      : ticketCategorizationSchemesRestGetSelector,
    {
      scheme_id: profileId
    }
  );

  const categories: RestTicketCategorizationCategory[] = (profile?.categories || []).filter(
    category => category?.id !== UNCATEGORIZED_ID_SUFFIX
  );

  if ((categories || []).length === 0) return <Empty />;

  return <CategoryRankComponent profile={profile} />;
};

export default CategoriesConfigurationPageContentBody;
