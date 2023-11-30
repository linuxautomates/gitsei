import { DependencyList, useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { configTablesListState, configTablesGetState } from "reduxConfigs/selectors/restapiSelector";
import { configTablesList, configTablesGet } from "reduxConfigs/actions/restapi";
import { v1 as uuid } from "uuid";
import { get } from "lodash";
import { convertFromTableSchema } from "configuration-tables/helper";

export const useTables = (
  method: "list" | "get",
  id?: string,
  withSchema?: boolean,
  versionId?: string,
  deps?: DependencyList,
  list_key?: string
) => {
  const dispatch = useDispatch();

  const listRestState = useSelector(configTablesListState);
  const getRestState = useSelector(configTablesGetState);

  const [apiId, setApiId] = useState<string>("");
  const [apiData, setApiData] = useState<any>();
  const [apiLoading, setApiLoading] = useState<boolean>(false);
  const [apiLoaded, setApiLoaded] = useState<boolean>(false);

  useEffect(
    () => {
      if (method === "list") {
        const id = list_key || uuid();
        dispatch(configTablesList({}, id));
        setApiData(undefined);
        setApiId(id);
        setApiLoading(true);
        setApiLoaded(false);
      } else if (id) {
        let getId = id;
        if (withSchema) {
          getId = `${id}?expand=schema,rows,history`;
        }
        if (versionId) {
          getId = `${id}/revisions/${versionId}?expand=schema,rows,history`;
        }
        dispatch(configTablesGet(getId));
        setApiData(undefined);
        setApiId(getId as string);
        setApiLoading(true);
        setApiLoaded(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    deps ? [...deps] : []
  );

  useEffect(() => {
    if (method === "list") {
      const restList = get(listRestState, [apiId], {});
      const loading = get(restList, ["loading"], true);
      const error = get(restList, ["error"], true);
      if (!loading && !error) {
        const data = get(restList, ["data", "records"], []);
        setApiData(data);
        setApiLoading(false);
        setApiLoaded(true);
      }

      if (!loading && error) {
        setApiData([]);
        setApiLoading(false);
        setApiLoaded(true);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [listRestState]);

  useEffect(() => {
    if (method === "get") {
      const restGet = get(getRestState, [apiId], {});
      const loading = get(restGet, ["loading"], true);
      const error = get(restGet, ["error"], true);

      if (!loading && !error) {
        const data = get(restGet, ["data"], {});
        setApiData(convertFromTableSchema(data));
        setApiLoading(false);
        setApiLoaded(true);
      }

      if (!loading && error) {
        setApiData({});
        setApiLoading(false);
        setApiLoaded(true);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [getRestState]);

  return [apiLoading, apiData, apiLoaded];
};
