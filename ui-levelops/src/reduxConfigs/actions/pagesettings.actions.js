export const SET_PAGE = "SET_PAGE";
export const CLEAR_PAGE = "CLEAR_PAGE";
export const SET_PAGE_SETTINGS = "SET_PAGE_SETTINGS";
export const CLEAR_PAGE_SETTINGS = "CLEAR_PAGE_SETTINGS";
export const SET_PAGE_BUTTON_ACTION = "SET_PAGE_BUTTON_ACTION";
export const SET_PAGE_DROPDOWN_ACTION = "SET_PAGE_DROPDOWN_ACTION";
export const SET_PAGE_SWITCH_ACTION = "SET_PAGE_SWITCH_ACTION";
export const SET_PAGE_SELECT_DROPDOWN_ACTION = "SET_PAGE_SELECT_DROPDOWN_ACTION";

export const setPage = (page, settings) => ({
  type: SET_PAGE,
  page: page,
  settings: settings
});

export const clearPage = page => ({
  type: CLEAR_PAGE,
  page: page
});

export const setPageSettings = (path, settings) => ({
  type: SET_PAGE_SETTINGS,
  path,
  settings
});

export const clearPageSettings = path => ({
  type: CLEAR_PAGE_SETTINGS,
  path
});

export const setPageButtonAction = (path, btnType, btnAttributes) => ({
  type: SET_PAGE_BUTTON_ACTION,
  path,
  btnType,
  btnAttributes
});

export const setPageDropDownAction = (path, btnType, btnAttributes) => ({
  type: SET_PAGE_DROPDOWN_ACTION,
  path,
  btnType,
  btnAttributes
});

export const setPageSelectDropDownAction = (path, btnType, btnAttributes) => ({
  type: SET_PAGE_SELECT_DROPDOWN_ACTION,
  path,
  btnType,
  btnAttributes
});

export const setPageSwitchAction = (path, btnType, btnAttributes) => ({
  type: SET_PAGE_SWITCH_ACTION,
  path,
  btnType,
  btnAttributes
});
