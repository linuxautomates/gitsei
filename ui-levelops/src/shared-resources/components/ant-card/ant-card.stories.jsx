import React from "react";
import { AntCard } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Card",
  component: AntCard
};

export const Card = () => <AntCard title="Check">kdno</AntCard>;
export const CardWithHeadBody = () => (
  <AntCard title="Default size card">
    <p>Card content</p>
    <p>Card content</p>
    <p>Card content</p>
  </AntCard>
);
