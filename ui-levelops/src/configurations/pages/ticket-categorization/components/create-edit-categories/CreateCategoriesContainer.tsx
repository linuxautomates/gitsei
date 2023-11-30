import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import React, { useCallback, useEffect, useMemo } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { ticketCategorizationSchemesRestCreateSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { WebRoutes } from "routes/WebRoutes";
import { NEW_SCHEME_ID, TICKET_CATEGORIZATION_SCHEME } from "../../constants/ticket-categorization.constants";
import CategoryCreateEditModal from "../../containers/create-edit-profile/CategoryCreateEditModel";
import { EIConfigurationTabs } from "../../types/ticketCategorization.types";

const CreateCategoriesContainer: React.FC<{ categoryId: string }> = ({ categoryId }) => {
  const history = useHistory();
  const dispatch = useDispatch();
  const restScheme: RestTicketCategorizationScheme = useParamSelector(ticketCategorizationSchemesRestCreateSelector);

  const restCreateState = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "create",
    uuid: "0"
  });

  const category = useMemo(() => {
    return (
      restScheme?.categories?.find(category => category?.id === categoryId) ||
      new RestTicketCategorizationCategory({ color: restScheme?.getTopUnusedColor() })
    );
  }, [restScheme, categoryId]);

  useEffect(() => {
    const { data } = restCreateState || {};
    if (data?.id !== undefined) {
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID));
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "create", "0"));
      history.push(WebRoutes.ticket_categorization.scheme.edit(data?.id));
    }
  }, [restCreateState]);

  const handleUpdate = useCallback(
    (updatedCategory: any) => {
      restScheme.addCategory(updatedCategory);
      dispatch(
        genericRestAPISet({ ...restScheme.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID)
      );
    },
    [restScheme]
  );

  const handleSave = useCallback(() => {
    history.push(WebRoutes.ticket_categorization.scheme.edit(NEW_SCHEME_ID, EIConfigurationTabs.CATEGORIES));
  }, [restScheme]);

  return (
    <CategoryCreateEditModal
      profile={restScheme}
      handleSave={handleSave}
      handleUpdate={handleUpdate}
      category={category}
    />
  );
};

export default CreateCategoriesContainer;
