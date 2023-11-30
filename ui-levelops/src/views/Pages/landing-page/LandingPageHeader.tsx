import React, { useEffect, useMemo } from "react";
import { Layout } from "antd";
import "./LandingPage.scss";
import LocalStoreService from "services/localStoreService";
import queryString from "query-string";
import { AntButton, AntIcon, AntSelect, AntText, Banner } from "shared-resources/components";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getGenericPageLocationSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { RouteComponentProps, useHistory, useLocation } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { setPageSelectDropDownAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { get } from "lodash";
import { SvgIconComponent } from "shared-resources/components/svg-icon/svg-icon.component";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { getBaseUrl, WORKSPACE_PATH, DEFAULT_ROUTES } from "constants/routePaths";
import { USERROLES } from "routes/helper/constants";
import { CATEGORY_DAHBOARD_MSG, CATGOERY_MSG, DASHBOARD_MSG, WORKSPACE_MSG } from "./constant";
import { SELECT_DROPDOWN_MAPPING } from "core/containers/header/select-dropdowns";
import { SelectDropdownKeys } from "core/containers/header/select-dropdowns/select-dropdown.types";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { compareCurrentDate } from "utils/dateUtils";

const { Header } = Layout;
const LandingPageHeader: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const localStorage = new LocalStoreService();
  const userName = localStorage.getFirstName();
  const history = useHistory();
  const location = useLocation();
  const dispatch = useDispatch();
  const params = queryString.parse(location.search) as any;
  const userRole: string = localStorage.getUserRbac() as string;
  const hasManageAccess = userRole?.toLowerCase() === USERROLES.ADMIN;
  const isTrialUser = useSelector(isSelfOnboardingUser);

  const getSelectedWorkspaceState = useSelector(getSelectedWorkspace);
  const pagePathnameState = useParamSelector(getGenericPageLocationSelector, {
    location: location?.pathname
  });

  const pagePathnameBannerState = useParamSelector(getGenericPageLocationSelector, {
    location: `${location?.pathname}/banner`
  });

  useEffect(() => {
    const addDashboardIdToLocalStorage = window.isStandaloneApp
      ? getRBACPermission(PermeableMetrics.ADD_DASHBOARD_ID_TO_LOCALSTORAGE)
      : true;
    if (addDashboardIdToLocalStorage) {
      // @ts-ignore
      history.push(DEFAULT_ROUTES[userRole]?.());
    }
  }, []);
  const dropDownButtons = useMemo(() => {
    const page = pagePathnameState;
    if (!page.hasOwnProperty("select_dropdown")) {
      return null;
    }
    return Object.keys(page.select_dropdown).map((btnType, index) => {
      const btn = page?.select_dropdown[btnType];
      const label = btn?.label;
      if (SELECT_DROPDOWN_MAPPING.hasOwnProperty(btnType as SelectDropdownKeys)) {
        return React.createElement(SELECT_DROPDOWN_MAPPING[btnType as SelectDropdownKeys] as any, {
          ...(btn ?? {})
        });
      }
      return (
        <div className="workspace-select-div">
          <AntText className="workspace-select-div__text">{label}</AntText>
          <AntSelect
            className="workspace-select-div__select"
            value={btn.value || btn?.selected_option?.value}
            options={btn.options || []}
            onChange={(e: string) => {
              const selectedOption = btn.options.find((option: any) => option.value === e);
              dispatch(
                setPageSelectDropDownAction(location.pathname, btnType, {
                  hasClicked: true,
                  value: e,
                  selected_option: selectedOption
                })
              );
            }}
          />
        </div>
      );
    });
  }, [pagePathnameState]);

  const message = useMemo(() => {
    const id = get(getSelectedWorkspaceState, ["id"], undefined);
    const category = get(params, ["ou_category_id"], undefined);
    const dashboardId = get(params, ["dashboard_id"], undefined);

    const userMsg = `Welcome ${userName || ""}`;
    switch (!!userName) {
      case id === undefined:
        return `${userMsg}, ${WORKSPACE_MSG}`;
      case id !== undefined && category === undefined:
        return CATGOERY_MSG;
      case category !== undefined && dashboardId !== undefined:
        return CATEGORY_DAHBOARD_MSG;
      case category !== undefined:
        return DASHBOARD_MSG;
      default:
        return `${userMsg}!`;
    }
  }, [pagePathnameState, getSelectedWorkspaceState?.id, params]);

  const goBack = () => {
    const id = get(getSelectedWorkspaceState, ["id"], undefined);
    const category = get(params, ["ou_category_id"], undefined);
    const dashboardId = get(params, ["dashboard_id"], undefined);

    if (id !== undefined && category === undefined && dashboardId === undefined) {
      dispatch(setPageSettings(location.pathname, {}));
      dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, -1));
      window.localStorage.removeItem(SELECTED_WORKSPACE_ID);
      history.push(getBaseUrl());
    } else {
      history.goBack();
    }
  };

  const showBanner = useMemo(() => {
    return !pagePathnameBannerState?.hide && compareCurrentDate("02-28-2023");
  }, [pagePathnameBannerState]);

  return (
    <>
      {showBanner && !get(getSelectedWorkspaceState, ["id"], undefined) && <Banner />}
      <Header className="landing-page-header bg">
        <div className="logo-text">
          <div className="flex">
            {window.isStandaloneApp && get(getSelectedWorkspaceState, ["id"], undefined) && (
              <span className="back-icon" onClick={goBack}>
                <SvgIconComponent icon="arrowLeftCircle" />
              </span>
            )}
            <span className="message">{message}</span>
          </div>
        </div>
        {window.isStandaloneApp && (
          <>
            {dropDownButtons}
            {!dropDownButtons && hasManageAccess && (
              <span
                className={isTrialUser ? "manage-button-disable" : "manage-button"}
                onClick={
                  isTrialUser
                    ? () => {}
                    : () => {
                        history.push(`${getBaseUrl()}${WORKSPACE_PATH}`);
                      }
                }>
                <AntButton>
                  {" "}
                  <AntIcon type="setting"></AntIcon>&nbsp;Manage Projects{" "}
                </AntButton>
              </span>
            )}
          </>
        )}
      </Header>
    </>
  );
};

export default React.memo(LandingPageHeader);
