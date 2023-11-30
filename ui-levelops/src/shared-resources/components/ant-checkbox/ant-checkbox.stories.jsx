import React from "react";
import { AntCheckbox } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Checkbox",
  component: AntCheckbox
};

export const Checkbox = () => <AntCheckbox>Default State</AntCheckbox>;
export const Checked = () => <AntCheckbox checked>Checked</AntCheckbox>;
export const Disabled = () => <AntCheckbox disabled>Disabled</AntCheckbox>;
export const DisabledChecked = () => (
  <AntCheckbox disabled checked>
    Disabled and checked
  </AntCheckbox>
);
