import { combineReducers } from "redux";
import automationRulesReducer from "./reducers/automationRulesReducer";
import integrationReducer from "./reducers/integrationReducer";
import errorReducer from "./reducers/errorReducer";
import sessionReducer from "./reducers/sessionReducer";
import paginationReducer from "./reducers/paginationReducer";
import restapiReducer from "./reducers/restapiReducer";
import tabCountReducer from "./reducers/tabCountReducer";
import { formReducer } from "./reducers/formReducer";
import { pageSettingsReducer } from "./reducers/pagesettings.reducer";
import { CLEAR_STORE } from "./actions/actionTypes";
import widgetLibraryReducer from "./reducers/widgetLibraryReducer";
import requiredFieldReducer from "./reducers/requiredFields";
import cachedStateReducer from "./reducers/cachedStateReducer";
import entitlementsReducer from "./reducers/entitlementsReducer";
import trellisProfileReducer from "./reducers/trellisProfileReducer";
import workSpaceReducer from "./reducers/workspace/workspaceReducer";
import storage from "redux-persist/lib/storage";
import widgetAPIReducer from "./reducers/widgetAPIReducer";
import widgetGraphAPIReducer from "./reducers/widgetGraphAPIReducer";
import cachedIntegrationReducer from "./reducers/integration/integrationReducer";
import workflowProfileReducer from "./reducers/workflowProfileReducer";
import workflowProfileByOuReducer from "./reducers/workflowProfileByOuReducer";
import cicdJobRunParamsReducer from "./reducers/cicdJobRunParamsReducer";
import unSavedChangesReducer from "./reducers/unSavedChangesReducer";

const appReducer = combineReducers({
  //ui: uiReducer,
  automationRulesReducer,
  integrationReducer,
  errorReducer,
  //dndReducer,
  sessionReducer,
  paginationReducer,
  restapiReducer,
  tabCountReducer,
  formReducer,
  pageSettingsReducer,
  widgetLibraryReducer,
  requiredFieldReducer,
  cachedStateReducer,
  entitlementsReducer,
  trellisProfileReducer,
  workSpaceReducer,
  widgetAPIReducer,
  widgetGraphAPIReducer,
  cachedIntegrationReducer,
  workflowProfileReducer,
  workflowProfileByOuReducer,
  cicdJobRunParamsReducer,
  unSavedChangesReducer
});

export const rootReducer = (state, action) => {
  if (action.type === CLEAR_STORE) {
    storage.removeItem("persist:root:cachedState");

    return appReducer(undefined, action);
  }

  return appReducer(state, action);
};
