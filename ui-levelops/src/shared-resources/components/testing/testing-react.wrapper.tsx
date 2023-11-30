import * as React from "react";
import { render, RenderOptions, RenderResult } from "@testing-library/react";
import { Provider } from "react-redux";
import store from "../../../store";

export interface TestRenderOptions extends Omit<RenderOptions, "queries"> {
  store?: any;
}
export function testRender(ui: React.ReactElement, options?: TestRenderOptions): RenderResult {
  return render(<Provider store={store}>{ui}</Provider>, options);
}
