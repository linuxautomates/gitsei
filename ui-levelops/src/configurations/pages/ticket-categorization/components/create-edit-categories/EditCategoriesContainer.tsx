import { notification } from "antd";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import React, { useCallback, useEffect, useMemo } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { ticketCategorizationSchemesRestGetSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { WebRoutes } from "routes/WebRoutes";
import {
  PROFILE_NOT_FOUND,
  PROFILE_UPDATED_SUCCESSFULLY,
  TICKET_CATEGORIZATION_SCHEME
} from "../../constants/ticket-categorization.constants";
import CategoryCreateEditModal from "../../containers/create-edit-profile/CategoryCreateEditModel";

const EditCategoriesContainer: React.FC<{ schemeId: string; categoryId: string }> = ({ schemeId, categoryId }) => {
  const dispatch = useDispatch();
  const history = useHistory();

  const restScheme: RestTicketCategorizationScheme = useParamSelector(ticketCategorizationSchemesRestGetSelector, {
    scheme_id: schemeId
  });

  const category = useMemo(() => {
    return (
      restScheme?.categories?.find(category => category?.id === categoryId) || new RestTicketCategorizationCategory({})
    );
  }, [restScheme, categoryId]);

  const restUpdateState = useParamSelector(getGenericRestAPISelector, {
    uri: TICKET_CATEGORIZATION_SCHEME,
    method: "update",
    uuid: schemeId
  });

  useEffect(() => {
    if (restScheme.id === undefined) {
      notification.error({ message: PROFILE_NOT_FOUND });
      history.push(WebRoutes.ticket_categorization.list());
    }
  }, []);

  useEffect(() => {
    const { data } = restUpdateState || {};
    if (data?.changed === true) {
      notification.success({ message: PROFILE_UPDATED_SUCCESSFULLY });
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "update", schemeId));
      dispatch(genericRestAPISet({}, TICKET_CATEGORIZATION_SCHEME, "get", schemeId));
      history.goBack();
    }
  }, [restUpdateState]);

  const handleUpdate = useCallback(
    (updatedCategory: any) => {
      restScheme.addCategory(updatedCategory);
      dispatch(genericRestAPISet({ ...restScheme.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "get", schemeId));
    },
    [schemeId, restScheme]
  );

  const handleSave = useCallback(() => {
    dispatch(genericRestAPISet({ ...restScheme.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "get", schemeId));
    history.goBack();
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

export default EditCategoriesContainer;
