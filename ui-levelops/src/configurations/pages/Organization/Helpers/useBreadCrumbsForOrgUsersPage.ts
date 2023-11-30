import { getBaseUrl, ORGANIZATION_USERS_ROUTES } from "constants/routePaths";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation } from "react-router-dom";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";
import { WebRoutes } from "../../../../routes/WebRoutes";

export const useBreadCrumbsForOrgUsersPage = () => {
  const dispatch = useDispatch();
  const changesSelector = useSelector(unSavedChangesSelector);
  const history = useHistory();
  const location = useLocation();

  const validateRedirect = (path: string) => {
    if (changesSelector.dirty && location.pathname === `${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`) {
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
      label: "Contributors",
      path: WebRoutes.organization_users_page.root(),
      customOnClick: (e: any) => {
        e.preventDefault();
        validateRedirect(WebRoutes.organization_users_page.root());
      }
    }
  ];
};
