import React from "react";
import { AntSteps } from "shared-resources/components";
import { Icon } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Steps",
  components: AntSteps
};

const steps = [
  {
    title: "Finished",
    description: "This is a description."
  },
  {
    title: "In progress",
    description: "This is a description.",
    subTitle: "Left 00:00:08"
  },
  {
    title: "Waiting",
    description: "This is a description."
  }
];

const smallSteps = [
  {
    title: "Finished"
  },
  {
    title: "In progress"
  },
  {
    title: "Waiting"
  }
];

const stepIcon = [
  {
    status: "finish",
    title: "Login",
    icon: <Icon type="user" />
  },
  {
    status: "error",
    title: "Verification",
    icon: <Icon type="solution" />
  },
  {
    status: "process",
    title: "Pay",
    icon: <Icon type="loading" />
  },
  {
    status: "wait",
    title: "Done",
    icon: <Icon type="smile-o" />
  }
];

export const Steps = () => <AntSteps current={0} steps={steps} />;

export const StepsSmall = () => <AntSteps size="small" current={1} steps={smallSteps} />;

export const StepsErrorStatus = () => <AntSteps current={1} status="error" steps={smallSteps} />;

export const StepsWithIconAndStatus = () => <AntSteps steps={stepIcon} />;

export const StepsDotStyle = () => <AntSteps progressDot current={1} steps={smallSteps} />;

export const StepsVertical = () => <AntSteps direction="vertical" steps={steps} />;

export const StepsDotVertical = () => <AntSteps progressDot current={1} direction="vertical" steps={smallSteps} />;
