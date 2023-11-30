import React from "react";
import { Steps } from "antd";

const { Step } = Steps;

export const AntStepsComponent = props => {
  const makeStep = () => {
    const data = props.steps;
    return data.map((step, i) => {
      return <Step key={i} {...step}></Step>;
    });
  };

  return <Steps {...props}>{makeStep()}</Steps>;
};
