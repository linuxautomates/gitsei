import React, { useEffect, useRef, useState } from "react";
import { RouteComponentProps } from "react-router-dom";
import queryString from "query-string";
import { useDispatch } from "react-redux";
import { integrationsBulk, productAggsGet, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";
import Loader from "components/Loader/Loader";
import { SnykReport } from "reports/components";
import { workspaceApiClear, workSpaceGet } from "reduxConfigs/actions/workspaceActions";
import { getGenericWorkSpaceMethodSelector } from "reduxConfigs/selectors/workspace/workspace.selector";

const UNKNOWN = "UNKNOWN";

const IntegrationReportDetailsPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const resultId = useRef<string | undefined>(undefined);
  const workspaceId = useRef<string | undefined>(undefined);
  const [loading, setLoading] = useState<boolean>(false);
  const [productName, setProductName] = useState<string>(UNKNOWN);
  const [integrationName, setIntegrationName] = useState<string>(UNKNOWN);
  const [integrationLoading, setIntegrationLoading] = useState<boolean>(false);
  const [productLoading, setProductLoading] = useState<boolean>(false);
  const dispatch = useDispatch();
  const productsAggsGetState = useParamSelector(getGenericRestAPISelector, {
    uri: "product_aggs",
    method: "get",
    uuid: resultId.current
  });
  const integrationBulkState = useParamSelector(getGenericRestAPISelector, {
    uri: "integrations",
    method: "bulk",
    uuid: "0"
  });

  const workspaceGetState = useParamSelector(getGenericWorkSpaceMethodSelector, {
    method: "get"
  });

  useEffect(() => {
    const values = queryString.parse(props.location.search);
    const id = values?.report;
    if (id) {
      dispatch(productAggsGet(id));
      resultId.current = id as string;
      setLoading(true);
    }
    return () => {
      dispatch(restapiClear("product_aggs", "get", "-1"));
      dispatch(workspaceApiClear(workspaceId.current as string, "get"));
      dispatch(restapiClear("integrations", "bulk", "0"));
    };
  }, []);

  useEffect(() => {
    if (loading) {
      const curLoading = get(productsAggsGetState, ["loading"], true);
      const curError = get(productsAggsGetState, ["error"], false);
      if (!curLoading && !curError) {
        const data = get(productsAggsGetState, ["data"], {});
        if (!!data.product_id) {
          dispatch(workSpaceGet(data.product_id));
          workspaceId.current = data.product_id;
          setProductLoading(true);
        }
        if (data?.integration_ids.length > 0) {
          dispatch(
            integrationsBulk({
              filter: {
                integration_ids: data?.integration_ids
              }
            })
          );
          setIntegrationLoading(true);
        }
        setLoading(false);
      }
    }
  }, [productsAggsGetState]);

  useEffect(() => {
    if (productLoading) {
      let curLoading = false;
      let productNames = [];
      const data = get(productsAggsGetState, ["data"], {});
      const productId = data?.product_id;
      const loading = get(workspaceGetState, [productId, "loading"], true);
      const error = get(workspaceGetState, [productId, "error"], false);
      if (!loading && !error) {
        productNames.push(get(workspaceGetState, [productId, "data", "name"], ""));
      } else {
        curLoading = true;
      }
      setProductName(curLoading ? UNKNOWN : productNames.join(", "));
      setProductLoading(curLoading);
    }
  }, [workspaceGetState]);

  useEffect(() => {
    if (integrationLoading) {
      const loading = get(integrationBulkState, ["loading"], true);
      const error = get(integrationBulkState, ["error"], false);
      let integrations = UNKNOWN;
      if (!loading && !error) {
        const data = get(integrationBulkState, ["data"], {});
        integrations = data?.records?.map((record: any) => record?.name).join(",");
      }
      setIntegrationLoading(loading);
      setIntegrationName(integrations);
    }
  }, [integrationBulkState]);

  const report = {
    ...get(productsAggsGetState, ["data"], {}),
    products: productName,
    integrations: integrationName
  };

  if (loading) return <Loader />;

  return <SnykReport report={report} />;
};

export default IntegrationReportDetailsPage;
