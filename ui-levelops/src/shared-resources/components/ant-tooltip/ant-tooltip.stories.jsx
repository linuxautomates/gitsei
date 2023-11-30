import React from "react";
import { AntTooltip } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Tooltip"
};

export const Tooltip = () => (
  <AntTooltip defaultVisible title="tooltip text">
    Tooltip will show on mouse enter
  </AntTooltip>
);

export const TooltipPlacementTopLeft = () => (
  <>
    <AntTooltip placement="topLeft" title="Tooltip text">
      Top Left
    </AntTooltip>
  </>
);

export const TooltipPlacementRight = () => (
  <>
    <AntTooltip placement="right" title="Tooltip text">
      Right
    </AntTooltip>
  </>
);
