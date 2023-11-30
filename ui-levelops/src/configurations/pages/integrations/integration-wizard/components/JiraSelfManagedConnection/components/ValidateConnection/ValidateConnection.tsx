import {
  Button,
  ButtonVariation,
  Color,
  Container,
  FontVariation,
  Icon,
  Layout,
  StepProps,
  Text
} from "@harness/uicore";
import CardV2 from "components/CardV2/CardV2";
import { noop } from "lodash";
import React from "react";
import { JiraSelfManagedDTO } from "../Configuration/Configuration.types";
import css from "./ValidateConnection.module.scss";

interface ValidateConnectionProps {
  name: string;
}

export default function ValidateConnection(
  props: StepProps<JiraSelfManagedDTO> & ValidateConnectionProps
): JSX.Element {
  const { previousStep, prevStepData } = props;
  return (
    <Layout.Vertical className={css.configurationPanel}>
      <Container>
        <Text font={{ variation: FontVariation.H4 }} padding={{ bottom: "medium" }}>
          {"Verify Connection"}
        </Text>
        <CardV2>
          <Text font={{ variation: FontVariation.BODY2, weight: "light" }} padding={{ bottom: "small" }}>
            {"Download YAML and apply the file on your on-prem infrastructure"}
          </Text>
          <Button
            variation={ButtonVariation.PRIMARY}
            icon={"download-box"}
            iconProps={{ color: Color.WHITE }}
            type="submit"
            text={"Download YAML"}
            className={css.downloadYamlBtn}
            onClick={noop}
          />
          <hr className={css.separator} />
          <Layout.Horizontal>
            <Icon name="loading" padding={{ right: "medium" }} />
            <Text font={{ variation: FontVariation.BODY2, weight: "light" }} padding={{ bottom: "small" }}>
              {"Waiting for connection. It generally takes 4-5 minutes to receive a heartbeat after applying YAML"}
            </Text>
          </Layout.Horizontal>
        </CardV2>
      </Container>
      <Layout.Horizontal spacing={"small"}>
        <Button
          variation={ButtonVariation.SECONDARY}
          type="submit"
          text={"Back"}
          icon={"chevron-left"}
          onClick={() => previousStep?.(prevStepData)}
        />
        <Button
          variation={ButtonVariation.PRIMARY}
          type="submit"
          text={"Done"}
          rightIcon="chevron-right"
          intent="success"
        />
      </Layout.Horizontal>
    </Layout.Vertical>
  );
}
