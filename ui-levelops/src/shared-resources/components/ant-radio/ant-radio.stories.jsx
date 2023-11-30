import React from "react";
import { AntRadio } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Radio"
};

export const Radio = () => <AntRadio>Default State</AntRadio>;

export const CheckedRadio = () => <AntRadio checked>Checked</AntRadio>;
export const DisabledRadio = () => <AntRadio disabled>Disabled</AntRadio>;
export const DisabledCheckedRadio = () => (
  <AntRadio disabled checked>
    Disabled Checked
  </AntRadio>
);
