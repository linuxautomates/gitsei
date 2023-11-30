import React from "react";
import { AntTagCheckable } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Tag Checkable"
};

export const TagCheckable = () => (
  <>
    <AntTagCheckable>Tag 1</AntTagCheckable>
    <AntTagCheckable>Tag 2</AntTagCheckable>
  </>
);
