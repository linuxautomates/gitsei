import React, { useContext, useEffect, useMemo } from "react";
import { useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import "./WidgetExplorerPage.scss";
import { useHeader } from "../../../../custom-hooks/useHeader";
import { generateExploreWidgetPageBreadcrumbs } from "../../../../utils/dashboardUtils";
import { DashboardWidgetResolverContext } from "../../context";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { AntCol, AntRow } from "../../../../shared-resources/components";
import WidgetThemes from "./themes/WidgetThemes";
import WidgetLibrary from "./library/WidgetLibrary";
import { WebRoutes } from "../../../../routes/WebRoutes";

interface WidgetExplorerPageProps extends RouteComponentProps {}

const CANCEL_ACTION_KEY = "action_cancel";

const ACTION_BUTTONS = {
  [CANCEL_ACTION_KEY]: {
    type: "ghost",
    label: "Cancel",
    hasClicked: false
  }
};

const WidgetExplorerPage: React.FC<WidgetExplorerPageProps> = ({ history, location }) => {
  const { setupHeader, onActionButtonClick } = useHeader(location.pathname);
  const { dashboardId } = useContext(DashboardWidgetResolverContext);
  const dashboard = useSelector(selectedDashboard);

  useEffect(() => {
    const pageSettings = {
      title: "Widget Explorer",
      action_buttons: ACTION_BUTTONS,
      bread_crumbs: generateExploreWidgetPageBreadcrumbs(
        {},
        dashboardId,
        dashboard?.name,
        "Explore Widgets",
        location?.search
      ),
      showDivider: true,
      newWidgetExplorerHeader: true
    };
    setupHeader(pageSettings);
  }, [dashboard]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    onActionButtonClick((action: string) => {
      switch (action) {
        case CANCEL_ACTION_KEY:
          history.replace({
            // @ts-ignore
            pathname: WebRoutes.dashboard.details(undefined, dashboardId),
            search: location?.search || undefined
          });
          return {
            hasClicked: false
          };
        default:
          return null;
      }
    });
  }, [onActionButtonClick]); // eslint-disable-line react-hooks/exhaustive-deps

  const memoizedGutter = useMemo(() => [30, 30], []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <AntRow gutter={memoizedGutter} className="h-100 pb-0">
      <AntCol span={6} className="h-100">
        <WidgetLibrary />
      </AntCol>
      <AntCol span={18} className="h-100">
        <WidgetThemes />
      </AntCol>
    </AntRow>
  );
};

export default WidgetExplorerPage;
