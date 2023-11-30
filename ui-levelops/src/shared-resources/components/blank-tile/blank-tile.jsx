import React from "react";
import { AntCardComponent as AntCard } from "../ant-card/ant-card.component";
import "./blank-tile.style.scss";

export const BlankTileComponent = props => {
  return <AntCard className="blank-tile" {...props} />;
};
