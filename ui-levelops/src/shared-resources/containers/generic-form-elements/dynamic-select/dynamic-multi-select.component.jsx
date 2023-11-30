import React from "react";
import { DynamicSelectComponent } from "./dynamic-select.component";

export const DynamicMultiSelectWrapper = props => <DynamicSelectComponent {...props} mode={"multiple"} />;
