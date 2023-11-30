import { getReportPageSettings, pageSettings } from "../selectors/pagesettings.selector";
import {
  clearPage,
  clearPageSettings,
  setPage,
  setPageButtonAction,
  setPageDropDownAction,
  setPageSelectDropDownAction,
  setPageSettings,
  setPageSwitchAction
} from "../actions/pagesettings.actions";

export const mapReportPageSettingsStateToProps = state => {
  return {
    page_settings: getReportPageSettings(state)
  };
};

export const mapPageSettingsStateToProps = state => {
  return {
    page_settings: pageSettings(state)
  };
};

export const mapPageSettingsDispatchToProps = dispatch => {
  return {
    setPage: (page, settings) => dispatch(setPage(page, settings)),
    clearPage: page => dispatch(clearPage(page)),
    setPageSettings: (path, settings) => dispatch(setPageSettings(path, settings)),
    clearPageSettings: path => dispatch(clearPageSettings(path)),
    setPageButtonAction: (path, btnType, attribute) => dispatch(setPageButtonAction(path, btnType, attribute)),
    setPageDropDownAction: (path, btnType, attribute) => dispatch(setPageDropDownAction(path, btnType, attribute)),
    setPageSwitchAction: (path, btnType, attribute) => dispatch(setPageSwitchAction(path, btnType, attribute)),
    setPageSelectDropDownAction: (path, btnType, attribute) =>
      dispatch(setPageSelectDropDownAction(path, btnType, attribute))
  };
};
