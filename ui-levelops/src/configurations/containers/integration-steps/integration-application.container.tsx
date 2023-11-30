import React, { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Modal } from "antd";
import { AntParagraph } from "../../../shared-resources/components";
import { ALL_SCM_INTEGRATIONS, getIntegrationUrlMap } from "constants/integrations";
import "./integration.style.scss";
import { AutomatedIntegrationCard } from "../../components/integration-card";
import { integrationType } from "reduxConfigs/actions/integrationActions";
import { RouteComponentProps } from "react-router-dom";
import { createMarkup } from "../../../utils/stringUtils";
import { getIntegrationPage } from "constants/routePaths";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, IntegrationAnalyticsActions } from "dataTracking/analytics.constants";
import {
  CLOUD_BASED_INTEGRATIONS,
  ENTERPRISE_BASED_INTEGRATIONS,
  NEW_ONBOARDING_INTEGRATIONS
} from "configurations/pages/self-onboarding/constants";
import { WebRoutes } from "routes/WebRoutes";
import { get } from "lodash";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import sanitizeHtml from "sanitize-html";
import { IntegrationTypes } from "constants/IntegrationTypes";
import SelectIntegrationTypeModal from "./components/SelectIntegrationTypeModal/SelectIntegrationTypeModal";

interface IntegrationApplicationContainerProps extends RouteComponentProps {
  enableDashboardRoutes?: boolean;
  session_rbac?: string;
  handleClick?: (position: any) => void;
  displayDefaultTrialIntegration?: boolean;
  disabled?: boolean;
  showNotFullySupportedIntegration?: boolean;
}
export const IntegrationApplicationContainer: React.FC<IntegrationApplicationContainerProps> = (
  props: IntegrationApplicationContainerProps
) => {
  const dispatch = useDispatch();
  const [show_details, setShowDetails] = useState<boolean>(false);
  const [details, setDetails] = useState<any>("");
  const [newIntegrationType, setNewIntegrationType] = useState<string>("");
  const isTrialUser = useSelector(isSelfOnboardingUser);
  const entScmIntegration = useHasEntitlements(Entitlement.SETTING_SCM_INTEGRATIONS);
  const entScmIntegrationCountExcced = useHasEntitlements(
    Entitlement.SETTING_SCM_INTEGRATIONS_COUNT_3,
    EntitlementCheckType.AND
  );
  const allowGithubAction = useHasEntitlements(Entitlement.ALLOW_GITHUB_ACTION_TILE, EntitlementCheckType.AND);
  const integrationMap = getIntegrationUrlMap();

  //TODO - this feature flag will be coming from backend once its available.
  const SEI_NEW_ONBOARDING_INTEGRATIONS = false;

  const handleOnInstallationClick = (integrationtype: string) => {
    // GA EVENT
    emitEvent(AnalyticsCategoryType.INTEGRATION, IntegrationAnalyticsActions.INTEGRATION_APP_START, integrationtype);
    dispatch(integrationType(integrationtype));
    let url = `${getIntegrationPage()}/add-integration-page`;

    if (NEW_ONBOARDING_INTEGRATIONS.includes(integrationtype) && SEI_NEW_ONBOARDING_INTEGRATIONS) {
      setNewIntegrationType(integrationtype);
      return;
    }
    if (ENTERPRISE_BASED_INTEGRATIONS.includes(integrationtype)) {
      url = `${getIntegrationPage()}/types?application=${get(
        integrationMap,
        [integrationtype, "application"],
        integrationtype
      )}`;
    }
    if (CLOUD_BASED_INTEGRATIONS.includes(integrationtype)) {
      let doNotSplitIntegration = get(integrationMap, [integrationtype, "doNotSplitIntegration"], false);
      const integration = doNotSplitIntegration ? integrationtype : integrationtype.split("_")[0];
      url = WebRoutes.self_onboarding.root(integration, 0);
    }
    props.history.push(url);
  };

  const detailsModal = () => {
    // sanitizing data to prevent xss
    const sanitizedDetails = sanitizeHtml(details);
    return (
      <Modal
        title={"Details"}
        visible={show_details}
        width={400}
        closable={true}
        footer={false}
        onCancel={() => {
          setShowDetails(false);
          setDetails("");
        }}>
        <AntParagraph className="desc-normal" style={{ overflowWrap: "break-word" }}>
          <div className="integration-description" dangerouslySetInnerHTML={createMarkup(sanitizedDetails)} />
        </AntParagraph>
      </Modal>
    );
  };

  const integrations = () => {
    const _integrationsSort = (first: any, second: any) => {
      if (first === second) {
        return 0;
      }
      if (first === "custom") {
        return 1;
      }
      if (second === "custom") {
        return -1;
      }
      return first.localeCompare(second);
    };
    let integrationTypes = Object.keys(integrationMap);
    if (isTrialUser) {
      if (props.displayDefaultTrialIntegration) {
        integrationTypes = integrationTypes.filter(
          // @ts-ignore
          integrationType => integrationMap[integrationType].displayForTrialUser
        );
      } else {
        integrationTypes = integrationTypes.filter(
          // @ts-ignore
          integrationType => !integrationMap[integrationType].displayForTrialUser
        );
      }
    }
    if (props.showNotFullySupportedIntegration) {
      integrationTypes = integrationTypes.filter(
        // @ts-ignore
        integrationType => integrationMap[integrationType].notFullySupportedIntegration
      );
    } else {
      integrationTypes = integrationTypes.filter(
        // @ts-ignore
        integrationType => !integrationMap[integrationType].notFullySupportedIntegration
      );
    }
    const items = integrationTypes.sort(_integrationsSort).map((integrationType: any, index: any) => {
      // @ts-ignore
      if (integrationMap[integrationType].hide) {
        return null;
      }
      if (IntegrationTypes.GITHUB_ACTIONS === integrationType && !allowGithubAction) {
        return null;
      }
      const isSCMIntegration = ALL_SCM_INTEGRATIONS.includes(integrationType as any);
      return (
        <AutomatedIntegrationCard
          key={index}
          integration_type={integrationType}
          onDetailsClick={(item: any) => {
            setShowDetails(true);
            //@ts-ignore
            setDetails(integrationMap[item].description);
          }}
          onInstallClick={handleOnInstallationClick}
          scmDisabled={
            props.disabled ||
            (window.isStandaloneApp && isSCMIntegration && (!entScmIntegration || entScmIntegrationCountExcced))
          }
        />
      );
    });

    return <div className={"integration-cards-grid"}>{items}</div>;
  };

  return (
    <>
      {detailsModal()}
      {integrations()}
      {newIntegrationType ? (
        <SelectIntegrationTypeModal
          integrationType={newIntegrationType}
          handleClose={() => setNewIntegrationType("")}
        />
      ) : null}
    </>
  );
};

export default React.memo(IntegrationApplicationContainer);
