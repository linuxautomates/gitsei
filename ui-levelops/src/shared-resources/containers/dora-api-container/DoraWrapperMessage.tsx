import React, { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AntButton, AntText, SvgIcon } from "shared-resources/components";
import { getWorkflowProfilePage } from "constants/routePaths";
import { INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_FUNCTION_CALL } from "./constants";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import DORAErrorDrawer from "./DORAErrorDrawer";
import { Tooltip } from "antd";
import { useIsDashboardReadonly } from "custom-hooks/HarnessPermissions/useIsDashboardReadonly";

interface DoraWrapperMessageProps {
  link?: string;
  message: string;
  widgetTitle: string;
  messageAfterLinkToAppend?: string;
  redirectWidgetEdit?: (value: any) => void;
  reasons?: string[];
}

const DoraWrapperMessage: React.FC<DoraWrapperMessageProps> = ({
  link,
  message,
  messageAfterLinkToAppend,
  redirectWidgetEdit,
  widgetTitle,
  reasons
}) => {
  const [showDrawer, setShowDrawer] = useState<boolean>(false);
  const hasHarnessEditAccess = useIsDashboardReadonly();
  const hasEditAccess = window.isStandaloneApp
    ? getRBACPermission(PermeableMetrics.ADMIN_WIDGET_EXTRAS)
    : !hasHarnessEditAccess;
  const clickMsg = useMemo(() => {
    const workflowProfileLink = getWorkflowProfilePage();
    switch (link) {
      case workflowProfileLink:
        return (
          <Link to={link} style={{ textDecoration: "underline" }}>
            Click here
          </Link>
        );
      case INVALID_CONFIGURATION_FOR_CHANGE_FAILURE_RATE_FUNCTION_CALL:
        return (
          <span onClick={redirectWidgetEdit}>
            <a style={{ textDecoration: "underline" }}>Click here</a>
          </span>
        );
      default:
        return null;
    }
  }, [link, redirectWidgetEdit]);

  return (
    <div className="centered h-100 flex">
      <AntText disabled>
        {message}{" "}
        {link &&
          (hasEditAccess ? (
            <>
              Please {clickMsg} {messageAfterLinkToAppend}
            </>
          ) : (
            "Please reach out to your admin."
          ))}
      </AntText>
      {reasons?.length && (
        <Tooltip title="Learn about the possible reasons">
          <AntButton className={"ant-btn-outline columnFilterButton"} onClick={() => setShowDrawer(true)}>
            <SvgIcon className="reports-btn-icon" icon="questionCircle" />
          </AntButton>
        </Tooltip>
      )}
      <DORAErrorDrawer
        isVisible={showDrawer}
        onClose={() => setShowDrawer(false)}
        widgetTitle={widgetTitle}
        errorMessage={message}
        reasons={reasons || []}
      />
    </div>
  );
};

export default DoraWrapperMessage;
