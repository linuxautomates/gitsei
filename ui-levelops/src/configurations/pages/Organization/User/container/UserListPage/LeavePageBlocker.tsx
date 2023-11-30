import React, { useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Prompt, useHistory, useLocation } from "react-router-dom";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";

const LeavePageBlocker: React.FC<{ when: any; path: string }> = ({ when, path }) => {
  const changesSelector = useSelector(unSavedChangesSelector);
  const message = "There are unsaved changes which will be lost if you proceed.\nAre you sure you want to proceed?";
  const location = useLocation();
  const history = useHistory();
  const dispatch = useDispatch();
  useEffect(() => {
    if (!when) return () => {};
    const beforeUnloadCallback = (event: any) => {
      event.preventDefault();
      event.returnValue = message;
      return message;
    };
    window.addEventListener("beforeunload", beforeUnloadCallback);
    return () => {
      window.removeEventListener("beforeunload", beforeUnloadCallback);
      dispatch(setUnSavedChanges({ show_modal: false, dirty: false, onCancel: "" }));
    };
  }, [when, message]);

  const renderPrompt = useMemo(() => {
    return (
      <>
        {changesSelector.dirty &&
        changesSelector?.show_modal === false &&
        location.pathname === path &&
        history.action === "POP" ? (
          <Prompt when={when} message={message} />
        ) : (
          <></>
        )}
      </>
    );
  }, [changesSelector.dirty, changesSelector?.show_modal === false, location.pathname, history.action === "POP"]);

  return { ...renderPrompt };
};

export default LeavePageBlocker;
