import { Color, FontVariation, Icon, Text } from "@harness/uicore";
import React from "react";

interface LabelWithLockProps {
  label: string;
  isLocked?: boolean;
}

const LabelWithLock = ({ label, isLocked }: LabelWithLockProps) => {
  return (
    <>
      <Icon
        name={isLocked ? "lock" : "unlock"}
        color={isLocked ? Color.RED_600 : Color.GREEN_600}
        margin={{ left: "small", right: "small" }}
      />
      <Text font={{ variation: FontVariation.BODY }}>{label}</Text>
    </>
  );
};

export default LabelWithLock;
