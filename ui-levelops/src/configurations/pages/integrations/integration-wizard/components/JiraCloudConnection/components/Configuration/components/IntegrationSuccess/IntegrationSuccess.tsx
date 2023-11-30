import { Color, Icon, Layout, Text } from "@harness/uicore";
import CardV2 from "components/CardV2/CardV2";
import React from "react";
import { IntegrationData } from "../../Configuration.types";
import css from "./IntegrationSuccess.module.scss";
import IntegrationSuccessImage from "../../../../../../../../../../assets/svg/IntegrationSuccess.svg";

interface IntegrationSuccessProps {
  integrationData: IntegrationData;
}

export default function IntegrationSuccess(props: IntegrationSuccessProps): JSX.Element {
  const { integrationData } = props;
  const { integrationId, connectedAt } = integrationData || {};
  return (
    <>
      <Layout.Horizontal padding={{ bottom: "small" }}>
        <Icon name="success-tick" size={24} />
        <Text padding={{ left: "small" }} color={Color.BLACK}>
          {"Established connection with the harness.atlassian.com/jira"}
        </Text>
      </Layout.Horizontal>
      <Layout.Horizontal padding={{ bottom: "small" }}>
        <Icon name="success-tick" size={24} />
        <Text padding={{ left: "small" }} color={Color.BLACK}>
          {"Authentication successful"}
        </Text>
      </Layout.Horizontal>
      <Layout.Horizontal padding={{ bottom: "small" }}>
        <Icon name="success-tick" size={24} />
        <Text padding={{ left: "small" }} color={Color.BLACK}>
          {"Pre-flag check passed"}
        </Text>
      </Layout.Horizontal>
      <Layout.Horizontal padding={{ bottom: "small" }}>
        <Icon name="success-tick" size={24} />
        <Text padding={{ left: "small", bottom: "small" }} color={Color.BLACK}>
          {"Integration has been created."}
        </Text>
      </Layout.Horizontal>
      <CardV2 className={css.container}>
        <Layout.Horizontal padding={{ top: "medium", bottom: "medium" }}>
          <IntegrationSuccessImage />
          <Layout.Vertical padding={{ left: "medium" }}>
            <Text color={Color.GREEN_900}>{"Great! You have successfully connected to Jira!"}</Text>
            {/* <Text color={Color.GREEN_900}>
              {"Please proceed to select your Jira projects, or you may come back later to continue. "}
            </Text> */}
          </Layout.Vertical>
        </Layout.Horizontal>
        <Layout.Vertical background={Color.WHITE} padding={"medium"} border={{ radius: 4, color: "#D9DAE5" }}>
          <Text font={{ weight: "semi-bold" }} color={Color.BLACK}>
            {"Connection Information"}
          </Text>
          <Layout.Horizontal>
            <Text>{"Integration ID:"}</Text>
            <Text font={{ weight: "semi-bold" }} color={Color.BLACK} padding={{ left: "medium" }}>
              {integrationId}
            </Text>
          </Layout.Horizontal>
          <Layout.Horizontal>
            <Text>{"Connected at:"}</Text>
            <Text font={{ weight: "semi-bold" }} color={Color.BLACK} padding={{ left: "medium" }}>
              {connectedAt}
            </Text>
          </Layout.Horizontal>
        </Layout.Vertical>
      </CardV2>
    </>
  );
}
