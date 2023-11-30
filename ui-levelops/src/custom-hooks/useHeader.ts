import { useCallback, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";

// update it.
type ButtonState = {
  hasClicked?: boolean;
  progressLabel?: string;
  type?: string;
  label?: string;
  showProgress?: boolean;
  disabled?: boolean;
};
type HeaderSettings = {
  title: string;
  description?: string;
  action_buttons?: any;
  bread_crumbs?: any[];
  withBackButton?: boolean;
  bread_crumbs_position?: string;
  showDivider?: boolean;
  showBottomSeparator?: boolean;
  showFullScreenBottomSeparator?: boolean;
  headerClassName?: string;
};

// todo : handle dropdown and select buttons.
export function useHeader(pathname: string) {
  const [action_buttons, setActionButtons] = useState<string[]>([]);
  const dispatch = useDispatch();

  const pageSettingState = useSelector(getPageSettingsSelector);

  const setupHeader = useCallback(
    (pageSettings: HeaderSettings) => {
      const actionButtonKeys = Object.keys(pageSettings.action_buttons);
      setActionButtons(actionButtonKeys);
      dispatch(setPageSettings(pathname, pageSettings));
    },
    [pathname]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const changeButtonState = useCallback(
    (actionKey: string, state: ButtonState) => {
      dispatch(setPageButtonAction(pathname, actionKey, state));
    },
    [pathname]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleActionButtonClick = useCallback(
    (callback: (action: string) => Object | null) => {
      const actionButtons = pageSettingState[pathname]?.action_buttons;
      if (!actionButtons) {
        return;
      }
      action_buttons.map((action: string) => {
        if (actionButtons[action]?.hasClicked) {
          const buttonConfiguration = callback(action);
          if (buttonConfiguration) {
            dispatch(setPageButtonAction(pathname, action, buttonConfiguration));
          }
        }
      });
    },
    [pageSettingState, pathname]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  return { setupHeader, changeButtonState, onActionButtonClick: handleActionButtonClick };
}
