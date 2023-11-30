import React from "react";
import { AntDatepicker } from "shared-resources/components";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Datepicker",
  components: AntDatepicker
};

export const Datepicker = () => <AntDatepicker />;

export const Rangepicker = () => <AntDatepicker type="range" />;

export const Monthpicker = () => <AntDatepicker type="month" />;

export const Weekpicker = () => <AntDatepicker type="week" />;
