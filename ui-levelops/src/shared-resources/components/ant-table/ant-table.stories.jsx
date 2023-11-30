import React from "react";
import { AntTable } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Table"
};

export const Table = () => <AntTable bordered />;

export const SmallTable = () => <AntTable bordered size="small" />;
