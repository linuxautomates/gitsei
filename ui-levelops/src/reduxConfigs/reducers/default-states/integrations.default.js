import { fromJS } from "immutable";

export const initialIntegrationsState = fromJS({
  create: {
    type: "",
    credentials: {},
    information: {},
    state: "",
    step: 0
  }
});
