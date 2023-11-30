import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { get } from "lodash";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { objectsList } from "reduxConfigs/actions/restapi/objectsActions";
import { getObjectsListSelector } from "reduxConfigs/selectors/objectsSelector";

export function useAutomationRules() {
  const dispatch = useDispatch();

  const [loadingObjects, setLoadingObjects] = React.useState(false);
  const objectListState = useSelector(getObjectsListSelector);
  const args = get(objectListState, ["0"], undefined);

  React.useEffect(() => {
    if (!(args && args.data)) {
      dispatch(objectsList());
      setLoadingObjects(true);
    } else {
      // console.log("Data already loaded, skipping fetch");
    }

    return () => {
      dispatch(restapiClear("objects", "list", "0"));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (args && args.data) {
    const records = args.data.records || [];

    return {
      loading: args.loading,
      data: records
    };
  }

  return {
    loading: loadingObjects
  };
}
