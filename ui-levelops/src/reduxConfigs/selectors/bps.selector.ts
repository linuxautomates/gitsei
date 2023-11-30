import { get } from "lodash";
import { createSelector } from "reselect";
import { getKBSelector } from "./restapiSelector";

export const kbGetState = createSelector(getKBSelector, state => get(state, ["get"], {}));
