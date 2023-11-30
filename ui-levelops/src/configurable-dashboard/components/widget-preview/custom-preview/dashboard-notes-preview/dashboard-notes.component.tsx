import React, { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import html2canvas from "html2canvas";
import cx from "classnames";
import { Button, Dropdown, Icon, Menu } from "antd";
import sanitizeHtml from "sanitize-html";
import { RestWidget } from "classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { DashboardNotesEditModal } from "dashboard/pages/dashboard-view/dashboard-view-modals/dashboard-notes-edit-modal.component";
import ConfirmationModalComponent from "shared-resources/components/confirmation-modal/ConfirmationModalComponent";
import { WIDGET_DELETE_WARNING } from "constants/formWarnings";
import { dashboardWidgetAdd, widgetDelete } from "reduxConfigs/actions/restapi/widgetActions";
import { dashboardWidgetsSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import Widget from "model/widget/Widget";
import { DashboardNotesReport } from "dashboard/pages/dashboard-view/constant";
import { WidgetsLoadingContext, WidgetSvgContext } from "dashboard/pages/context";
import { WebRoutes } from "routes/WebRoutes";

import "./dashboard-notes-preview.style.scss";
import { ProjectPathProps } from "classes/routeInterface";

interface DashboardNotesPreviewProps {
  dashboardId: string;
  widgetId: string;
  previewOnly?: boolean;
  hidden?: boolean;
}

const DEFAULT_HEIGHT = 225;

const DashboardNotesPreviewComponent: React.FC<DashboardNotesPreviewProps> = ({
  dashboardId,
  widgetId,
  previewOnly,
  hidden
}) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const projectParams = useParams<ProjectPathProps>();

  const widgetRef = useRef();

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const dashboard = useSelector(selectedDashboard);
  const widgets = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboard?.id
  });

  const [showMore, setShowMore] = useState<boolean>(false);
  const [showEditNotes, setShowEditNotes] = useState<boolean>(false);
  const [showDeleteModal, setShowDeleteModal] = useState<boolean>(false);
  const [hasOverflow, setHasOverflow] = useState<boolean>(false);
  // saving max height for animation
  const [maxHeight, setMaxHeight] = useState<string>("auto");
  const { setWidgetsLoading } = useContext(WidgetsLoadingContext);

  const { haveToTakeSnapshot, setSvg } = useContext(WidgetSvgContext);

  const toggleShowMore = () => {
    setShowMore(!showMore);
  };

  const contentRef = (_ref: any) => {
    if (maxHeight === "auto" && _ref?.clientHeight > DEFAULT_HEIGHT + 10) {
      setMaxHeight(`${_ref?.clientHeight}px`);
      setHasOverflow(true);
    }
  };

  const getImage = async (ref: any) => {
    // Fix: so that it do not block the ui rendering.
    setTimeout(async () => {
      const canvas = await html2canvas(ref.current);
      const img = canvas.toDataURL("image/png", 5.0);
      setSvg(widgetId, img);
    }, 0);
  };

  useEffect(() => {
    setWidgetsLoading(widgetId, false);
  }, []);

  useEffect(() => {
    maxHeight !== "auto" && setMaxHeight("auto");
  }, [widget.description]);

  useEffect(() => {
    haveToTakeSnapshot && !hidden && getImage(widgetRef);
  }, [haveToTakeSnapshot]);

  const content = useMemo(() => {
    const _height = hasOverflow ? (showMore ? maxHeight : `${DEFAULT_HEIGHT}px`) : maxHeight;
    // sanitizing data to prevent xss
    const sanitizeWidgetDescription = sanitizeHtml(widget?.description, {
      allowedAttributes: {
        span: ["style"],
        a: ["href", "target"]
      }
    });

    return (
      <div
        className="html-string-content"
        ref={contentRef}
        style={{ height: _height }}
        dangerouslySetInnerHTML={{ __html: sanitizeWidgetDescription }}
      />
    );
  }, [showMore, widget.description, hasOverflow, maxHeight]);

  const cloneTextWidget = useCallback(() => {
    if (dashboard) {
      const cloneWidget = Widget.newInstance(dashboard, DashboardNotesReport, widgets);
      if (!cloneWidget) {
        console.error("Error: Failed to create widget");
        return;
      }

      cloneWidget.name = "Documentation";
      cloneWidget.description = widget.description;

      dispatch(dashboardWidgetAdd(dashboard.id, cloneWidget.json));

      history.push(WebRoutes.dashboard.widgets.widgetsRearrange(projectParams, dashboard.id));
    }
  }, [widget, dashboard, widgets]);

  const onIconClick = useCallback(e => {
    e.preventDefault();
  }, []);

  const handleMenuClick = useCallback(payload => {
    switch (payload.key) {
      case "edit":
        setShowEditNotes(true);
        break;
      case "clone":
        cloneTextWidget();
        break;
      case "delete":
        setShowDeleteModal(true);
        break;
    }
  }, []);

  const menu = useMemo(
    () => (
      <Menu onClick={handleMenuClick} className="widget-actions-menu">
        <Menu.Item key={"edit"}>
          <Icon type="edit" onClick={onIconClick} />
          Edit
        </Menu.Item>
        <Menu.Item key={"clone"}>
          <Icon type="copy" onClick={onIconClick} />
          Clone
        </Menu.Item>
        <Menu.Item key={"delete"}>
          <Icon type="delete" onClick={onIconClick} />
          Delete
        </Menu.Item>
      </Menu>
    ),
    []
  );

  const onEditNotesClose = useCallback(() => {
    setShowEditNotes(false);
  }, []);

  const dashboardNotesModal = useMemo(
    () => (
      <DashboardNotesEditModal
        visible={showEditNotes}
        onClose={onEditNotesClose}
        widgetId={widgetId}
        dashboardId={dashboardId}
      />
    ),
    [showEditNotes]
  );

  const onDeleteOk = useCallback(() => {
    dispatch(widgetDelete(widgetId));
    setShowDeleteModal(false);
  }, []);

  const onDeleteCancel = useCallback(() => {
    setShowDeleteModal(false);
  }, []);

  const renderDeleteModal = useMemo(() => {
    return (
      <ConfirmationModalComponent
        text={WIDGET_DELETE_WARNING}
        onCancel={onDeleteCancel}
        onOk={onDeleteOk}
        visiblity={showDeleteModal}
      />
    );
  }, [showDeleteModal]);

  return (
    <>
      <div ref={widgetRef as any} className={cx("dashboard-notes-preview", { "dashboard-notes": !previewOnly })}>
        {previewOnly && <div className="preview-only-content">{content}</div>}
        {!previewOnly && (
          <div className="flex">
            <div className="dashboard-notes-preview__content">
              {content}
              {hasOverflow && (
                <Button
                  className="dashboard-notes-preview__content--content-view-button"
                  onClick={toggleShowMore}
                  icon={showMore ? "up" : "down"}
                  type="link">
                  {showMore ? "Show Less" : "Show More"}
                </Button>
              )}
            </div>
            <Dropdown
              overlay={menu}
              trigger={["click"]}
              placement="bottomRight"
              className="align-self-start notes-options-dropdown">
              <Button className="widget-extras">
                <Icon type={"more"} />
              </Button>
            </Dropdown>
          </div>
        )}
      </div>
      {dashboardNotesModal}
      {renderDeleteModal}
    </>
  );
};

export default React.memo(DashboardNotesPreviewComponent);
