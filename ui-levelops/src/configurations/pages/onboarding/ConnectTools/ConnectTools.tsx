import { Container, Text, Color, Layout, Collapse, IconProps, FontVariation } from "@harness/uicore";
import React from "react";
import css from "./ConnectTools.module.scss";
import ConnectToolsImage from "./../../../../assets/svg/ConnectTools.svg";

export default function ConnectTools(): JSX.Element {
  return (
    <Layout.Horizontal>
      <Layout.Vertical className={css.leftContainer} padding={"xlarge"}>
        <Text color={Color.GREY_900} font={{ size: "large", weight: "bold" }}>
          {"Connect your tools"}
        </Text>
        <Text color={Color.GREY_700} font={{ size: "medium" }} padding={{ top: "xlarge", bottom: "xlarge" }}>
          {
            "With SEI, you can easily connect to a wide range of tools and platforms, including source code repositories, build systems, testing frameworks, and deployment platforms."
          }
        </Text>
        <Text color={Color.GREY_500} font={{ size: "normal", weight: "bold" }} padding={{ bottom: "large" }}>
          {"Mandatory tools to connect"}
        </Text>
        <Collapse
          key={"issueManagement"}
          collapsedIcon="main-chevron-right"
          expandedIcon="main-chevron-down"
          iconProps={{ size: 12 } as IconProps}
          isRemovable={false}
          collapseClassName={css.collapseWrapper}
          collapseHeaderClassName={css.collapseHeader}
          heading={
            <Layout.Horizontal className={css.collapeHeaderContent}>
              <Text padding={{ left: "medium", right: "medium" }}>{"Icon"}</Text>
              <Layout.Vertical>
                <Text color={Color.BLACK} font={{ variation: FontVariation.H5 }}>
                  {"Issue Management Tool"}
                </Text>
                <Text color={Color.GREY_500} font={{ size: "small" }}>
                  {"Ticket based tools to keeping track of features"}
                </Text>
              </Layout.Vertical>
            </Layout.Horizontal>
          }>
          <Layout.Vertical padding={{ bottom: "medium" }}>
            <Text
              padding={{ top: "medium", bottom: "medium" }}
              color={Color.GREY_700}
              font={{ variation: FontVariation.BODY2 }}>
              {"Select a tool to Connect"}
            </Text>
            <Layout.Horizontal>
              <Container
                className={css.toolTile}
                padding={{ top: "medium", right: "medium", bottom: "medium" }}
                margin={{ right: "medium" }}>
                <Text padding={"medium"}>{"Icon"}</Text>
                <Layout.Vertical padding={{ left: "small" }}>
                  <Text color={Color.BLACK} font={{ variation: FontVariation.BODY2 }} padding={{ top: "small" }}>
                    {"Jira by Atlassian"}
                  </Text>
                  <Text padding={{ top: "small" }} className={css.toolInfo}>
                    {"Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking."}
                  </Text>
                </Layout.Vertical>
              </Container>
              <Container
                className={css.toolTile}
                padding={{ top: "medium", right: "medium", bottom: "medium" }}
                margin={{ right: "medium" }}>
                <Text padding={"medium"}>{"Icon"}</Text>
                <Layout.Vertical padding={{ left: "small" }}>
                  <Text color={Color.BLACK} font={{ variation: FontVariation.BODY2 }} padding={{ top: "small" }}>
                    {"Jira by Atlassian"}
                  </Text>
                  <Text padding={{ top: "small" }} className={css.toolInfo}>
                    {"Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking."}
                  </Text>
                </Layout.Vertical>
              </Container>
            </Layout.Horizontal>
          </Layout.Vertical>
        </Collapse>
        <Collapse
          key={"sourceCodeManagement"}
          collapsedIcon="main-chevron-right"
          expandedIcon="main-chevron-down"
          iconProps={{ size: 12 } as IconProps}
          isRemovable={false}
          collapseClassName={css.collapseWrapper}
          collapseHeaderClassName={css.collapseHeader}
          heading={
            <Layout.Horizontal className={css.collapeHeaderContent}>
              <Text padding={{ left: "medium", right: "medium" }}>{"Icon"}</Text>
              <Layout.Vertical>
                <Text color={Color.BLACK} font={{ variation: FontVariation.H5 }}>
                  {"Source Code Management"}
                </Text>
                <Text color={Color.GREY_500} font={{ size: "small" }}>
                  {"Codebase repo to review and merge codes"}
                </Text>
              </Layout.Vertical>
            </Layout.Horizontal>
          }>
          <Layout.Vertical padding={{ bottom: "medium" }}>
            <Text
              padding={{ top: "medium", bottom: "medium" }}
              color={Color.GREY_700}
              font={{ variation: FontVariation.BODY2 }}>
              {"Select a tool to Connect"}
            </Text>
            <Layout.Horizontal>
              <Container
                className={css.toolTile}
                padding={{ top: "medium", right: "medium", bottom: "medium" }}
                margin={{ right: "medium" }}>
                <Text padding={"medium"}>{"Icon"}</Text>
                <Layout.Vertical padding={{ left: "small" }}>
                  <Text color={Color.BLACK} font={{ variation: FontVariation.BODY2 }} padding={{ top: "small" }}>
                    {"Jira by Atlassian"}
                  </Text>
                  <Text padding={{ top: "small" }} className={css.toolInfo}>
                    {"Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking."}
                  </Text>
                </Layout.Vertical>
              </Container>
              <Container
                className={css.toolTile}
                padding={{ top: "medium", right: "medium", bottom: "medium" }}
                margin={{ right: "medium" }}>
                <Text padding={"medium"}>{"Icon"}</Text>
                <Layout.Vertical padding={{ left: "small" }}>
                  <Text color={Color.BLACK} font={{ variation: FontVariation.BODY2 }} padding={{ top: "small" }}>
                    {"Jira by Atlassian"}
                  </Text>
                  <Text padding={{ top: "small" }} className={css.toolInfo}>
                    {"Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking."}
                  </Text>
                </Layout.Vertical>
              </Container>
            </Layout.Horizontal>
          </Layout.Vertical>
        </Collapse>
      </Layout.Vertical>
      <Layout.Vertical padding={"xxlarge"} className={css.rightContainer}>
        <ConnectToolsImage />
        <Text font={{ variation: FontVariation.H4 }} color={Color.BLACK} padding={{ top: "large", bottom: "large" }}>
          {"We integrate with everything!"}
        </Text>
        <Text
          font={{ variation: FontVariation.BODY2, weight: "light" }}
          color={Color.BLACK}
          padding={{ top: "large", bottom: "large" }}>
          {"We know that teams live and die by their tools."}
        </Text>
        <Text
          className={css.connectToolsDecription}
          font={{ variation: FontVariation.BODY2, weight: "light" }}
          color={Color.BLACK}
          padding={{ top: "large", bottom: "large" }}>
          {
            "SEI is a powerful value stream management platform that offers seamless integration with virtually any tool, making it an incredibly versatile and flexible solution for software development teams."
          }
        </Text>
        <Text
          className={css.connectToolsDecription}
          font={{ variation: FontVariation.BODY2, weight: "light" }}
          color={Color.BLACK}
          padding={{ top: "large", bottom: "large" }}>
          {
            " With SEI, you can easily connect to a wide range of tools and platforms, including source code repositories, build systems, testing frameworks, and deployment platforms."
          }
        </Text>
      </Layout.Vertical>
    </Layout.Horizontal>
  );
}
