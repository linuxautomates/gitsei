// tags

import * as actions from "../actionTypes";

export const KBsGetOrCreate = (kbs, complete = null) => ({
  type: actions.KBS_GET_OR_CREATE,
  data: kbs,
  complete
});
