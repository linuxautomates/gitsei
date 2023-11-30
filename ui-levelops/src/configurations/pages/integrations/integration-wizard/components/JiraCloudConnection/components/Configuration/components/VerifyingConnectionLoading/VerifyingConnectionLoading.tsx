import { Color, FontVariation, Icon, Layout, Text } from "@harness/uicore";
import React from "react";
import css from "./../../Configuration.module.scss";
import BroadcomImage from "assets/svg/Broadcom.svg";

export default function VerifyingConnectionLoading(): JSX.Element {
  return (
    <>
      <Layout.Horizontal>
        <Icon name="loading" padding={"small"} />
        <Text
          font={{ variation: FontVariation.BODY2, size: "normal" }}
          color={Color.BLACK}
          padding={{ top: "small", bottom: "small", left: "small" }}>
          {"Waiting for connection"}
        </Text>
      </Layout.Horizontal>
      <Text className={css.seiInfo} padding={{ bottom: "medium" }}>
        {
          "“SEI provides us data driven insights on how to reduce devops friction on a very granular per scrum team level. This helps us maximize efficiency and remove pain points for engineers.”"
        }
      </Text>
      <BroadcomImage />
    </>
  );
}
