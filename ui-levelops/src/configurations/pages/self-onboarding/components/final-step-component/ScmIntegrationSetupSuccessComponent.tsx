import React, { useMemo, useState } from "react";
import queryString from "query-string";
import { useSelector } from "react-redux";
import { AntButton, AntModal } from "shared-resources/components";
import { Icon } from "antd";
import { useHistory, useLocation, useParams } from "react-router-dom";
import "./scmIntegrationSetupSuccessComponent.styles.scss";
import { USER_ID } from "constants/localStorageKeys";
import { getDefaultDashboardPath } from "utils/dashboardUtils";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { ProjectPathProps } from "classes/routeInterface";

const ScmIntegrationSetupSuccessComponent: React.FC = () => {
  const location = useLocation();
  const history = useHistory();
  const params = useParams();
  const projectParams = useParams<ProjectPathProps>();

  const { integration_id } = queryString.parse(location.search);
  const curUserMetricEditingStateId = `${localStorage.getItem(USER_ID)}@${integration_id}`;
  const curUserMetricEditingState = sessionStorage.getItem(curUserMetricEditingStateId) === "true";
  const [showDoraModal, setShowDoraModal] = useState<boolean>(!!integration_id);

  const isTrialUser = useSelector(isSelfOnboardingUser);

  const handleOnCancel = () => {
    setShowDoraModal(false);
    sessionStorage.setItem(curUserMetricEditingStateId, "true");
  };

  const renderFooter = useMemo(() => <AntButton onClick={handleOnCancel}>Done</AntButton>, []);

  if (curUserMetricEditingState) {
    history.replace(getDefaultDashboardPath(projectParams, (params as any)?.id));
    return null;
  }

  return (
    <AntModal
      visible={showDoraModal}
      centered={true}
      footer={renderFooter}
      closable={false}
      className="integration-success-modal">
      <div>
        <h2 className="flex justify-space-between align-center">Success!</h2>
        <p>Your app has been successfully integrated.</p>
        {isTrialUser && <p>You will receive an email when your insight is ready.</p>}
      </div>
    </AntModal>
  );
};

export default ScmIntegrationSetupSuccessComponent;
