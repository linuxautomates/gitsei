import { fromJS } from "immutable";
import {
  CLEAR_PAGE,
  CLEAR_PAGE_SETTINGS,
  SET_PAGE,
  SET_PAGE_BUTTON_ACTION,
  SET_PAGE_DROPDOWN_ACTION,
  SET_PAGE_SELECT_DROPDOWN_ACTION,
  SET_PAGE_SETTINGS,
  SET_PAGE_SWITCH_ACTION
} from "../actions/pagesettings.actions";

const initialState = fromJS({});

const getData = (state, action, type = "action_buttons") => {
  let data = state.getIn([action.path]);
  let btnData;
  if (data && data.hasOwnProperty(type) && data[type][action.btnType]) {
    btnData = data[type][action.btnType];
  }

  if (btnData && action.btnAttributes && Object.keys(action.btnAttributes).length > 0) {
    Object.keys(action.btnAttributes).forEach(attribute => {
      btnData = {
        ...btnData,
        [attribute]: action.btnAttributes[attribute]
      };
    });
  }

  if (data && btnData) {
    data = {
      ...data,
      [type]: {
        ...(data || {})[type],
        [action.btnType]: {
          ...btnData
        }
      }
    };
  }
  return data;
};

export const pageSettingsReducer = (state = initialState, action) => {
  // console.log(action);
  switch (action.type) {
    case SET_PAGE:
      return state.setIn([action.page], action.settings);
    case CLEAR_PAGE:
      return state.setIn([action.page], {});
    case SET_PAGE_SETTINGS:
      return state.setIn([action.path], action.settings);
    case SET_PAGE_BUTTON_ACTION:
      return state.setIn([action.path], getData(state, action));
    case SET_PAGE_DROPDOWN_ACTION:
      return state.setIn([action.path], getData(state, action, "dropdown_buttons"));
    case SET_PAGE_SWITCH_ACTION:
      return state.setIn([action.path], getData(state, action, "select_buttons"));
    case CLEAR_PAGE_SETTINGS:
      return state.setIn([action.path], {});
    case SET_PAGE_SELECT_DROPDOWN_ACTION:
      return state.setIn([action.path], getData(state, action, "select_dropdown"));
    default:
      return state;
  }
};
