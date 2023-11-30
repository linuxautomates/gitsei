import React, { useMemo } from "react";
import { useCallback } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { Button } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";
import { getBaseUrl, ORGANIZATION_USERS_ROUTES, getSettingsPage, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";
import { getIsStandaloneApp } from "helper/helper";

const HeaderBackButton: React.FC<{ link: string; goBackCallback: () => void | undefined }> = ({
  link,
  goBackCallback = undefined
}) => {
  const history = useHistory();
  const dispatch = useDispatch();
  const changesSelector = useSelector(unSavedChangesSelector);
  const location = useLocation();

  const displayBackButton = useMemo(
    () =>
      getIsStandaloneApp() ||
      (!link?.endsWith("/configuration") &&
        ![`${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`].includes(location.pathname)),
    []
  );

  const handleClick = useCallback(() => {
    if (
      changesSelector.dirty &&
      [`${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`, `${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}`].includes(
        location.pathname
      )
    ) {
      const backPath =
        location.pathname === `${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`
          ? getSettingsPage()
          : `${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES._ROOT}`;
      dispatch(
        setUnSavedChanges({
          show_modal: true,
          dirty: true,
          onCancel: () => {
            dispatch(setUnSavedChanges({ show_modal: false, dirty: false, onCancel: "" }));
            history.push(backPath);
          }
        })
      );
      return;
    }
    if (goBackCallback) {
      goBackCallback();
      return;
    }
    if (link === "generic") {
      history.goBack();
      return;
    }
    history.push(link);
    return;
  }, [link, goBackCallback]);

  if (displayBackButton)
    return (
      <Button type="default" shape="circle" icon="arrow-left" className="header-back-button" onClick={handleClick} />
    );
  return <></>;
};

export default React.memo(HeaderBackButton);
