import {
  Button,
  ButtonVariation,
  Color,
  Container,
  ExpandingSearchInput,
  FontVariation,
  Layout,
  StepProps,
  Text
} from "@harness/uicore";
import { noop } from "lodash";
import React from "react";
import ProjectsList from "./components/ProjectsList/ProjectsList";
import css from "./SelectProjects.module.scss";

interface StepData {
  newIntegrationType: string;
}

interface SelectProjectsProps {
  name: string;
  subtitle: string;
}

export default function SelectProjects(props: StepProps<StepData> & SelectProjectsProps): JSX.Element {
  const { previousStep, prevStepData } = props;
  const selectProjectsCount = `3/12`;
  return (
    <Layout.Vertical className={css.selectProjectsContainer}>
      <Layout.Vertical className={css.selectingProjectsPanel}>
        <Text font={{ variation: FontVariation.H3 }} color={Color.GREY_700} padding={{ bottom: "medium" }}>
          {"Which projects you want to add to Harness?"}
        </Text>
        <Text font={{ variation: FontVariation.BODY1 }} color={Color.GREY_500} padding={{ bottom: "medium" }}>
          {"SEI will only monitor the Jira projects once selected"}
        </Text>
        <Layout.Horizontal flex={{ alignItems: "center" }}>
          <Layout.Horizontal>
            <Text
              font={{ weight: "semi-bold" }}
              padding={{ top: "xsmall", right: "small", bottom: "xsmall" }}
              color={Color.GREY_900}>
              {"Projects selected"}
            </Text>
            <Container className={css.blueCircle} margin={{ right: "medium" }}>
              <Text font={{ weight: "semi-bold", size: "small" }} color={Color.WHITE}>
                {selectProjectsCount}
              </Text>
            </Container>
          </Layout.Horizontal>
          <ExpandingSearchInput alwaysExpanded placeholder={"Search JIRA Projects"} onChange={noop} width={450} />
        </Layout.Horizontal>
        <ProjectsList selectProjectsCount={selectProjectsCount} />
      </Layout.Vertical>
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
