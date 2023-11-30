import React, { useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import queryString from "query-string";
import { propelReportsGet, propelsList } from "reduxConfigs/actions/restapi";
import {
  getGenericMethodSelector,
  getGenericRestAPISelector,
  useParamSelector
} from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";
import { Descriptions } from "antd";
import { tableCell } from "utils/tableUtils";
import { AntCard, AntTable, AntText } from "shared-resources/components";
import { PROPELS_ROUTES, getBaseUrl } from "constants/routePaths";

const PropelReportDetailPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const reportId = useRef<string | undefined>();
  const [loading, setLoading] = useState<boolean>(true);
  const [runbookLoading, setRunbookLoading] = useState<boolean>(false);
  const [runbook, setRunbook] = useState<any | undefined>();
  const propelReportsState = useParamSelector(getGenericRestAPISelector, {
    uri: "propel_reports",
    method: "get",
    uuid: reportId.current
  });
  const prepelsState = useParamSelector(getGenericMethodSelector, {
    uri: "propels",
    method: "list"
  });
  const dispatch = useDispatch();

  useEffect(() => {
    const values = queryString.parse(props.location.search);
    const id = values?.report;
    if (id) {
      reportId.current = id as string;
      dispatch(propelReportsGet(id));
    }
  }, []);

  useEffect(() => {
    if (loading) {
      const curLoading = get(propelReportsState, ["loading"], true);
      const curError = get(propelReportsState, ["error"], false);
      if (!curLoading && !curError) {
        const data = get(propelReportsState, ["data"], {});

        if (data?.runbook_id) {
          dispatch(propelsList({ filter: { runbook_ids: [data.runbook_id] } }, data.runbook_id));
          setRunbookLoading(true);
        }
        setLoading(false);
      }
    }
  }, [propelReportsState]);

  useEffect(() => {
    if (runbookLoading) {
      const data = get(propelReportsState, ["data"], {});
      const runbookId = get(data, ["runbook_id"], undefined);
      const loading = get(prepelsState, [runbookId, "loading"], true);
      const error = get(prepelsState, [runbookId, "error"], false);
      if (!loading && !error) {
        const data = get(prepelsState, [runbookId, "data", "records"], []);
        setRunbookLoading(false);
        setRunbook(data[0]);
      }
    }
  }, [prepelsState]);

  const genericRender = useMemo(
    () => (item: any, record: any, index: number) => {
      if (!item) {
        return "";
      }
      if (Array.isArray(item)) {
        return item.join(", ");
      }
      if (item.constructor === Object) {
        return (
          <Descriptions layout={"horizontal"} column={1}>
            {Object.keys(item).map(key => (
              <Descriptions.Item label={key}>{JSON.stringify(item[key])}</Descriptions.Item>
            ))}
          </Descriptions>
        );
      }
      return item;
    },
    []
  );

  const getTableColumns = useMemo(() => {
    const data = get(propelReportsState, ["data"], {});
    const tableColumns = (data?.columns || [])
      .map((column: any) => ({
        title: column.replace(/_/g, " "),
        key: column,
        dataIndex: column,
        ellipsis: true,
        render: genericRender
      }))
      .filter((column: any) => column.key !== "section_title");
    return tableColumns;
  }, [propelReportsState]);

  const data = get(propelReportsState, ["data"], {});
  const sections = data?.section_titles || [];

  return (
    <AntCard
      className="pt-2 pb-10"
      title={`Propel Report ${tableCell("created_at", data?.created_at)}`}
      extra={
        <AntText strong>
          {`Runbook: `}
          <a href={`${getBaseUrl()}${PROPELS_ROUTES.PROPEL_EDITOR}?propel=${runbook?.permanent_id}`}>{runbook?.name}</a>
        </AntText>
      }>
      {sections?.length === 0 ? (
        <AntTable
          columns={getTableColumns}
          dataSource={data?.records || []}
          loading={get(propelReportsState, ["loading"], true)}
          pagination={{
            pageSize: 10,
            size: "small"
          }}
        />
      ) : (
        (sections || []).map((section: any) => (
          <AntTable
            title={() => {
              return (
                <AntText className="ml-10" strong>
                  {section}
                </AntText>
              );
            }}
            columns={getTableColumns}
            dataSource={(data?.records || []).filter((record: any) => record?.section_title === section)}
            loading={get(propelReportsState, ["loading"], true)}
            pagination={{
              pageSize: 10,
              size: "small"
            }}
          />
        ))
      )}
    </AntCard>
  );
};

export default PropelReportDetailPage;
