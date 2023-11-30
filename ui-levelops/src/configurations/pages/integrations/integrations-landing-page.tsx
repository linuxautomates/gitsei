import React, { useEffect, useState } from "react";
import { Tabs, Icon, Collapse } from "antd";
import queryString from "query-string";
import IntegrationsListPage from "./integrations-list/integrations-list.page";
import IntegrationApplicationContainer from "../../containers/integration-steps/integration-application.container";
import { SemiAutomatedIntegrationsList } from "../../components/integrations";
import "./integration-landing.styles.scss";
import { TAB_KEY } from "constants/fieldTypes";
import { RouteComponentProps } from "react-router-dom";
import { useSelector } from "react-redux";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { AntButton, AntText } from "shared-resources/components";
import { AUTOMATIC_DOCS_URL, SEMI_AUTOMATIC_DOCS_URL } from "constants/integrations";

interface IntegrationsLandingPageProps extends RouteComponentProps {}
const { TabPane } = Tabs;
const TALK_TO_EXCELLENCE_LINK =
  "https://calendly.com/d/d39-g5m-wqm/propelo-product-overview?utm_source=freetrial-integration";

const IntegrationsLandingPage: React.FC<IntegrationsLandingPageProps> = (props: IntegrationsLandingPageProps) => {
  const [activekey, setActiveKey] = useState<any>("available_integrations");
  const isTrialUser = useSelector(isSelfOnboardingUser);

  useEffect(() => {
    const active_key = queryString.parse(props.location?.search)?.[TAB_KEY];
    if (active_key !== activekey) {
      setActiveKey(active_key || "available_integrations");
    }
  }, [props]);

  const onTabChangeHandler = (key: any) => {
    props.history.push(`${props.location.pathname}?${TAB_KEY}=${key}`);
  };

  const availableIntegrations = () => {
    if (activekey !== "available_integrations") {
      return null;
    }
    if (isTrialUser) {
      return (
        <div className="integrations">
          <div className="more-description">
            The following integrations are available by contacting SEI.
            <AntButton type="primary" className="contact-button">
              <a href={TALK_TO_EXCELLENCE_LINK} target="_blank" rel="noopener noreferrer">
                Contact An Excellence Architect
              </a>
            </AntButton>
          </div>
          <div className="more-integrations">
            <div className="overlay"></div>
            <div>
              <IntegrationApplicationContainer {...props} disabled />
              <p className="integration-header">
                Semi-automated
                <Icon type="info-circle" theme="filled" className="icon" />
              </p>
              <SemiAutomatedIntegrationsList history={props.history} disabled />
            </div>
          </div>
        </div>
      );
    }
    return (
      <div className="integrations">
        <p className="integration-header">
          Automated
          <a className="link-icon-color" target="_blank" href={AUTOMATIC_DOCS_URL} rel="noopener noreferrer">
            <Icon type="info-circle" theme="filled" className="icon" />
          </a>
        </p>
        <IntegrationApplicationContainer {...props} />

        <p className="">
          <Collapse bordered={false}>
            <Collapse.Panel
              key={"other"}
              header={
                <div className="flex">
                  <AntText strong>Other Integrations</AntText>
                </div>
              }>
              <p>
                These integrations are now available with basic support, and we are actively working towards expanding
                their capabilities in the near future.
              </p>
              <IntegrationApplicationContainer {...props} showNotFullySupportedIntegration={true} />

              <p className="integration-header">
                Semi-automated
                <a className="link-icon-color" target="_blank" href={SEMI_AUTOMATIC_DOCS_URL} rel="noopener noreferrer">
                  <Icon type="info-circle" theme="filled" className="icon" />
                </a>
              </p>
              <SemiAutomatedIntegrationsList history={props.history} />
            </Collapse.Panel>
          </Collapse>
        </p>
      </div>
    );
  };

  return (
    <Tabs size={"small"} activeKey={activekey} onChange={onTabChangeHandler}>
      <TabPane key={"available_integrations"} tab={"Available Integrations"}>
        {availableIntegrations()}
      </TabPane>
      <TabPane key={"your_integrations"} tab={"Your Integrations"}>
        {activekey === "your_integrations" && <IntegrationsListPage {...props} />}
      </TabPane>
    </Tabs>
  );
};

export default IntegrationsLandingPage;
