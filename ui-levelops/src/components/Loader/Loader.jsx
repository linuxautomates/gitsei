import React from "react";
import { SvgIconComponent as SvgIcon } from "shared-resources/components/svg-icon/svg-icon.component";
import "shared-resources/style/mixin.scss";

const Loader = () => (
  <div className={`flex justify-center`}>
    <SvgIcon icon={"loading"} style={{ width: 30, height: 30, align: "center" }} />
  </div>
);

export default React.memo(Loader);
