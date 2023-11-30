import React, { useCallback } from "react";
import { maxBy } from "lodash";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import {
  ADD_CATEGORY,
  CATEGORIES,
  NEW_SCHEME_ID,
  TICKET_CATEGORIZATION_SCHEME,
  UNCATEGORIZED_ID_SUFFIX
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  ticketCategorizationSchemesRestCreateSelector,
  ticketCategorizationSchemesRestGetSelector
} from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { WebRoutes } from "routes/WebRoutes";
import { AntButton } from "shared-resources/components";
import TitleWithCount from "shared-resources/components/titleWithCount/TitleWithCount";
import { v1 as uuid } from "uuid";
import { CategoriesConfigurationPageContentProps } from "../CategoriesConfigurationPageContent";

const CategoriesConfigurationPageContentHeader: React.FC<CategoriesConfigurationPageContentProps> = ({ profileId }) => {
  const profile: RestTicketCategorizationScheme = useParamSelector(
    profileId === NEW_SCHEME_ID
      ? ticketCategorizationSchemesRestCreateSelector
      : ticketCategorizationSchemesRestGetSelector,
    {
      scheme_id: profileId
    }
  );
  const categories = (profile?.categories || []).filter(category => category?.id !== UNCATEGORIZED_ID_SUFFIX);
  const history = useHistory();
  const dispatch = useDispatch();

  const getMaxIndex = () => {
    const maxIndex = maxBy(categories, "index")?.index;
    return maxIndex === undefined ? 1 : maxIndex + 1;
  };

  const handleAddCategory = useCallback(() => {
    const unusedColor = profile?.getTopUnusedColor();
    const newCategory = new RestTicketCategorizationCategory({ color: unusedColor });
    newCategory.index = getMaxIndex();
    newCategory.id = uuid();
    profile?.addCategory(newCategory?.json);
    if (!!profile?.id)
      dispatch(genericRestAPISet({ ...profile?.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "get", profile?.id));
    else
      dispatch(
        genericRestAPISet({ ...profile?.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID)
      );
    history.push(WebRoutes.ticket_categorization.scheme.category.details(profileId, newCategory.id));
  }, [profile]);

  return (
    <div className="flex align-center justify-space-between">
      <TitleWithCount
        showZero={true}
        title={CATEGORIES}
        titleClass="category-count-text"
        count={(categories || []).length}
      />
      <div className="flex align-center">
        <AntButton type="default" onClick={handleAddCategory} className="add-category-button" disabled={!profile?.name}>
          {ADD_CATEGORY}
        </AntButton>
      </div>
    </div>
  );
};

export default CategoriesConfigurationPageContentHeader;
