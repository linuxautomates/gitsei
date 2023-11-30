import { getBaseUrl, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { useDispatch, useSelector } from "react-redux";
import { WebRoutes } from "routes/WebRoutes";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";
import { useHistory, useLocation } from "react-router-dom";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";

export const useBreadcumsForVelocitySchemePage = (schemeName: string) => {
  const dispatch = useDispatch();
  const changesSelector = useSelector(unSavedChangesSelector);
  const history = useHistory();
  const location = useLocation();

  const validateRedirect = (path: string) => {
    if (changesSelector.dirty && location.pathname === `${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}`) {
      dispatch(
        setUnSavedChanges({
          show_modal: true,
          dirty: true,
          onCancel: () => {
            dispatch(setUnSavedChanges({ show_modal: false, dirty: false, onCancel: "" }));
            history.push(path);
          }
        })
      );
    } else {
      history.push(path);
    }
  };
  return [
    {
      label: "Settings",
      path: WebRoutes.settings.root(),
      customOnClick: (e: any) => {
        e.preventDefault();
        validateRedirect(WebRoutes.settings.root());
      }
    },
    {
      label: "Workflow Profiles",
      path: WebRoutes.velocity_profile.list(),
      customOnClick: (e: any) => {
        e.preventDefault();
        validateRedirect(WebRoutes.velocity_profile.list());
      }
    },
    {
      label: schemeName
    }
  ];
};
