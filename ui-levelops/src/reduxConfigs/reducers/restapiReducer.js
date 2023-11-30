import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { cloneDeep, forEach, get, uniq, set, max } from "lodash";
import { v1 as uuid } from "uuid";

import {
  CLEAR_WIDGET_HISTORY,
  DASHBOARD_SET,
  DASHBOARD_WIDGET_ADD,
  DASHBOARD_WIDGET_CLONE,
  DASHBOARD_WIDGET_UPDATE,
  DELETE_ENTITIES,
  MAKE_WIDGETS_REVERSIBLE,
  RESTAPI_CLEAR,
  RESTAPI_CLEAR_ALL,
  RESTAPI_DATA,
  RESTAPI_ERROR,
  RESTAPI_ERROR_CODE,
  RESTAPI_LOADING,
  REVERT_WIDGET_CHANGES,
  SET_ENTITIES,
  SET_ENTITY,
  SET_SELECTED_ENTITY,
  GENERIC_REST_API_SET,
  SET_SELECTED_CHILD_ID,
  SET_CATEGORIES_VALUES_DATA,
  GENERIC_REST_API_LOADING,
  GENERIC_REST_API_ERROR,
  CLEAR_ORG_UNITS_LIST_FOR_INTEGRATION,
  TENANT_STATE,
  DEMO_WIGET_UPDATE
} from "../actions/actionTypes";
import { Model } from "../../model/Model";
import { WidgetType } from "../../dashboard/helpers/helper";
import { EFFORT_INVESTMENT_CATEGORIES_VALUES_NODE } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { isSanitizedValue } from "utils/commonUtils";

const INITIAL_STATE = {};

const getUpdatedDashboardState = (state, dashboardId, updatedDashboard, widgets) => {
  return {
    ...state,
    widgets: { ...(state?.widgets || {}), ...(widgets || {}) },
    dashboards: {
      ...(state?.dashboards || {}),
      get: {
        ...(state?.dashboards?.get || {}),
        [dashboardId]: {
          ...(state?.dashboards?.get?.[dashboardId] || {}),
          data: updatedDashboard
        }
      }
    }
  };
};

function initializeDict(state = {}, action) {
  if (!state.hasOwnProperty(action.uri)) {
    state[action.uri] = {};
  }
  try {
    if (action && "method" in action && "uri" in action) {
      if (action.method && !state[action.uri].hasOwnProperty(action.method)) {
        state[action.uri][action.method] = {};
      }
      // TODO dont know why action.id.toString() is erroring out
      if (action.method && !state[action.uri][action.method].hasOwnProperty(action.id)) {
        state[action.uri][action.method][action.id] = {};
      }
    }
  } catch (err) {
    console.error(state);
    console.error(action);
  }
  return state;
}

const restapiReducer = (state = INITIAL_STATE, action) => {
  // for any action, first split the url and see what you get
  // special handling needed for mapping
  // urls are of the format {version}/{url_type}/{id}
  const initialState = initializeDict({ ...state }, action);
  let newState = {};
  forEach(Object.keys(initialState), key => {
    if (!["undefined", "", "null"].includes(key)) {
      newState[key] = initialState?.[key];
    }
  });
  switch (action.type) {
    case RESTAPI_CLEAR_ALL:
      return INITIAL_STATE;
    case RESTAPI_LOADING:
      newState[action.uri][action.method][action.id].loading = action.loading;
      if (action.loading) {
        // if loading is true, the error is reset to false
        newState[action.uri][action.method][action.id].error = false;
        newState[action.uri][action.method][action.id].token = action.token;
      }
      //return JSON.parse(JSON.stringify(newState));
      //return newState;
      if (action.loading) {
        return newState;
      }
      return {
        ...newState,
        [action.uri]: {
          ...newState[action.uri],
          [action.method]: {
            ...newState[action.uri][action.method],
            [action.id]: {
              ...newState[action.uri][action.method][action.id],
              loading: action.loading,
              error: action.loading ? false : get(newState, [action.uri, action.method, action.id, "error"], false),
              token: action.loading ? action.token : null
            }
          }
        }
      };
    case RESTAPI_ERROR_CODE:
      newState[action.uri][action.method][action.id].error_code = action.error_code;
      return newState;
    case RESTAPI_ERROR:
      newState[action.uri][action.method][action.id].error = action.error;
      return newState;
    case RESTAPI_CLEAR:
      // this should be by method instead of id, or even by the action.method
      // safe delete
      // if action.id is -1, just delete every single id for the uri and method
      if (action.id === "-1") {
        return {
          ...newState,
          [action.uri]: {
            ...newState[action.uri],
            [action.method]: {}
          }
        };
      } else {
        if (newState?.[action.uri]?.[action.method]?.hasOwnProperty(action.id)) {
          newState[action.uri][action.method][action.id] = {};
          if (action.id !== "0") {
            return {
              ...newState,
              [action.uri]: {
                ...newState[action.uri],
                [action.method]: {
                  ...newState[action.uri][action.method],
                  [action.id]: {}
                }
              }
            };
          }
        }
      }
      return newState;
    case RESTAPI_DATA:
      newState[action.uri][action.method][action.id].data = action.data;
      return newState;
    case SET_SELECTED_ENTITY:
      const _oldData = get(newState, [action.uri], {});
      newState[action.uri] = {
        ..._oldData,
        ...action.data
      };
      return newState;
    case DEMO_WIGET_UPDATE:
      const old_demo_data = get(newState, [action.uri, "metadata", "demo_data", action.id], {});
      newState[action.uri]["metadata"]["demo_data"][action.id] = {
        ...old_demo_data,
        ...action.data
      };
      return newState;
    case SET_CATEGORIES_VALUES_DATA:
      newState[EFFORT_INVESTMENT_CATEGORIES_VALUES_NODE] = {
        ...get(newState || {}, [EFFORT_INVESTMENT_CATEGORIES_VALUES_NODE], {}),
        ...action.payload
      };
    case SET_SELECTED_CHILD_ID:
      newState[action.uri] = action.data;
      return newState;
    case SET_ENTITY:
      const oldData = get(newState, [action.uri, action.id], {});
      newState[action.uri] = {
        ...newState[action.uri],
        [action.id]: {
          ...Model.initState(),
          ...oldData,
          draft: false,
          ...action.data
        }
      };
      return newState;
    case SET_ENTITIES:
      let _data = action.data;
      if (!_data) {
        _data = [];
      }
      const list = _data.reduce((carry, item) => {
        if (item && item.id) {
          const oldData = get(newState, [action.uri, item.id], {});
          carry[item.id] = {
            ...Model.initState(),
            ...oldData,
            draft: false,
            ...item
          };
        }
        return carry;
      }, {});
      newState[action.uri] = {
        ...newState[action.uri],
        ...list
      };
      return newState;
    case DELETE_ENTITIES:
      const removedEntities = action.data;
      const allEntities = newState[action.uri];
      removedEntities.map(e => delete allEntities[e]);
      newState[action.uri] = {
        ...allEntities
      };
      return newState;

    case MAKE_WIDGETS_REVERSIBLE: {
      const ids = action.data;
      let allEntities = newState["widgets"];
      const clonedWidgets = {};
      ids.forEach(id => {
        const _widget = allEntities[id];
        clonedWidgets[`${id}-clone`] = { ..._widget };
        const _children = get(_widget, ["metadata", "children"], []);
        _children.forEach(child => {
          clonedWidgets[`${child}-clone`] = { ...allEntities[child] };
        });
      });
      newState["widgets-clones"] = clonedWidgets;
      return newState;
    }
    case REVERT_WIDGET_CHANGES: {
      const dashboardId = action.data.dashboard_id;
      const widgetIds = get(newState, ["dashboards", "get", dashboardId, "data", "widgets"], []);
      let allEntities = newState["widgets"];
      widgetIds.forEach(id => {
        const _newWidget = allEntities[id];
        const cloneId = `${id}-clone`;
        let clonedWidget = get(newState, ["widgets-clones", cloneId], undefined);
        if (clonedWidget) {
          if (_newWidget !== undefined) {
            // Delete new childs.
            const _children = get(_newWidget, ["metadata", "children"], []);
            _children.forEach(child => {
              delete allEntities[child];
            });
          }
          if (!_newWidget?.deleted) {
            allEntities[id] = clonedWidget;
          }
        }
      });
      //check for removing the widgets from store
      //when leaving configure flow (edit and modify)
      //and widget is a draft widget
      forEach(Object.keys(allEntities || {}), widgetId => {
        const _widget = allEntities[widgetId];
        if (_widget.draft === true) {
          delete allEntities[widgetId];
        }
      });
      newState["widgets"] = {
        ...allEntities
      };
      delete newState["widgets-clones"];
      return newState;
    }
    case CLEAR_WIDGET_HISTORY: {
      delete newState["widgets-clones"];
      return newState;
    }
    case DASHBOARD_SET:
      return {
        ...newState,
        [action.uri]: {
          ...(newState?.[action.uri] || {}),
          [action.method]: {
            ...(newState?.[action.uri]?.[action.method] || {}),
            [action.id]: {
              ...(newState?.[action.uri]?.[action.method]?.[action.id] || {}),
              data: action.data
            }
          }
        }
      };
    case DASHBOARD_WIDGET_ADD:
      const widgetId = action.widget.id;
      let oldDashboard = get(newState, ["dashboards", "get", action.dashboardId, "data"], {});
      const _widgets = oldDashboard.widgets;
      _widgets.push(widgetId);
      newState["dashboards"]["get"][action.dashboardId]["data"] = { ...oldDashboard, widgets: _widgets };
      newState["widgets"] = {
        ...newState.widgets,
        [widgetId]: action.widget
      };
      return newState;
    case DASHBOARD_WIDGET_UPDATE:
      const widget = get(newState, ["widgets", action.widgetId], {});
      forEach(Object.keys(action.form || {}), key => {
        const value = action.form?.[key];
        if (key === "metadata") {
          let newMetadata = cloneDeep(widget.metadata);
          forEach(Object.keys(value || {}), mKey => {
            const metaValue = value?.[mKey];
            if (
              mKey === "widget_type" &&
              widget.metadata.widget_type === WidgetType.COMPOSITE_GRAPH &&
              metaValue !== WidgetType.COMPOSITE_GRAPH
            ) {
              // Delete child of COMPOSITE_GRAPH on widget_type change...
              const _children = get(widget, ["metadata", "children"], []);
              _children.forEach(id => delete newState["widgets"][id]);
              newMetadata["children"] = [];
            }
            newMetadata[mKey] = metaValue;
          });
          widget.metadata = newMetadata;
        } else {
          widget[key] = action.form?.[key];
        }
      });
      newState["widgets"][action.widgetId] = { ...widget };
      newState["widgets"] = { ...newState["widgets"] };
      return newState;
    case DASHBOARD_WIDGET_CLONE:
      let dashboardClone = new RestDashboard(get(newState, ["dashboards", "get", action.dashboardId, "data"], {}));
      let widgets = get(newState, ["widgets"], {});
      const dashboardSpecificWidgets = Object.values(widgets)
        .map(w => new RestWidget(w))
        .filter(w => w.dashboard_id === action.dashboardId && !w?.hidden && !w.deleted);
      const widgetData = get(widgets, [action.widgetId], {});
      const clonedWidget = cloneDeep(widgetData);
      clonedWidget.id = action.cloneWidgetId;
      clonedWidget.name = "Copy of " + widgetData.name;
      clonedWidget.children = [];
      clonedWidget.draft = true;
      set(clonedWidget, ["metadata", "order"], (dashboardSpecificWidgets || []).length + 1);
      widgets[clonedWidget.id] = clonedWidget;
      const widgetChildren = uniq([...(widgetData?.children || []), ...(widgetData?.metadata?.children || [])]);
      let allWidgets = { ...widgets };
      const newChildIds = [];
      widgetChildren.forEach(widgetId => {
        const cloningChildWidget = get(widgets, [widgetId], undefined);
        if (!!cloningChildWidget) {
          const clonedChildWidget = cloneDeep(cloningChildWidget);
          clonedChildWidget.id = uuid();
          clonedChildWidget.name = "Copy of " + cloningChildWidget.name;
          clonedWidget.children.push(clonedChildWidget.id);
          newChildIds.push(clonedChildWidget.id);
          set(allWidgets, clonedChildWidget.id, clonedChildWidget);
        }
      });
      set(clonedWidget, ["metadata", "children"], newChildIds);
      const newWidgets = Object.keys(allWidgets).map(key => allWidgets[key]);
      dashboardClone.widgets = newWidgets;
      return getUpdatedDashboardState(newState, action.dashboardId, dashboardClone.json, allWidgets);
    case GENERIC_REST_API_SET:
      if (action.id !== "-1") {
        return {
          ...newState,
          [action.uri]: {
            ...(newState?.[action.uri] || {}),
            [action.method]: {
              ...(newState?.[action.uri]?.[action.method] || {}),
              [action.id]: {
                ...(newState?.[action.uri]?.[action.method]?.[action.id] || {}),
                data:
                  action.method === "list"
                    ? {
                        ...(newState?.[action.uri]?.[action.method]?.[action.id]?.data || {}),
                        records: action.data
                      }
                    : action.data
              }
            }
          }
        };
      } else {
        if (action.data.hasOwnProperty("-1") || newState?.[action?.uri]?.[action.method].hasOwnProperty("-1")) {
          delete action.data?.["-1"];
          delete newState?.[action?.uri]?.[action.method]?.["-1"];
        }
        return {
          ...newState,
          [action.uri]: {
            ...(newState?.[action.uri] || {}),
            [action.method]: {
              ...(newState?.[action?.uri]?.[action.method] || {}),
              ...action.data
            }
          }
        };
      }
    case GENERIC_REST_API_LOADING:
      return {
        ...newState,
        [action.uri]: {
          ...(newState?.[action.uri] || {}),
          [action.method]: {
            ...(newState?.[action.uri]?.[action.method] || {}),
            [action.id]: {
              error: undefined,
              ...(newState?.[action.uri]?.[action.method]?.[action.id] || {}),
              loading: action.loading
            }
          }
        }
      };
    case GENERIC_REST_API_ERROR:
      return {
        ...newState,
        [action.uri]: {
          ...(newState?.[action.uri] || {}),
          [action.method]: {
            ...(newState?.[action.uri]?.[action.method] || {}),
            [action.id]: {
              ...(newState?.[action.uri]?.[action.method]?.[action.id] || {}),
              loading: !isSanitizedValue(action.error),
              error: action.error
            }
          }
        }
      };
    case CLEAR_ORG_UNITS_LIST_FOR_INTEGRATION:
      const nextState = cloneDeep(newState);
      delete nextState[action?.uri];
      return nextState;
    default:
      return state;
  }
};

export default restapiReducer;
