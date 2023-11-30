import { Tag } from "antd";
import Loader from "components/Loader/Loader";
import { TableFiltersBEKeys } from "configurable-dashboard/components/configure-widget/configuration/table/constant";
import { useTables } from "custom-hooks";
import { get, isArray, upperCase } from "lodash";
import moment from "moment";
import React, { useCallback } from "react";
import { AntText } from "shared-resources/components";
import { toTitleCase, validMarkdownLink } from "utils/stringUtils";
import { isFilterDateTimeType } from "utils/timeUtils";

interface TableFiltersPreviewProps {
  filters: any;
  metaData: Record<string, any>;
}

const TableFiltersPreview: React.FC<TableFiltersPreviewProps> = props => {
  const { filters, metaData } = props;
  const tableId = get(metaData, ["tableId"], "");
  const [tableGetLoading, tableGetData] = useTables("get", tableId, true, undefined, [tableId]);

  const getFilterName = useCallback(
    (key: string) => {
      const columns = tableGetData?.columns ?? [];
      const _column = columns.find((col: any) => col.id === key);
      if (_column) return _column?.title;
      return undefined;
    },
    [tableGetData]
  );

  const getTransformedStringValue = (value: string) => {
    if (validMarkdownLink(value)) {
      const lastTitleIndex = value.lastIndexOf("]");
      return value.substring(1, lastTitleIndex);
    }
    return value;
  };

  const transformArrayData = (value: any[], key: string) => {
    if (key === TableFiltersBEKeys.COLUMNS) {
      return value.map(v => (v === "all" ? "All" : getFilterName(v)));
    }
    if (["string", "boolean"].includes(typeof value[0])) {
      return value.map((v: string | boolean) => getTransformedStringValue(v + ""));
    }
    return value;
  };

  if (tableGetLoading) return <Loader />;

  return (
    <>
      {Object.keys(filters).map((filter_key: string) => {
        const filterLabel = getFilterName(filter_key) ?? toTitleCase(filter_key);
        const label = (
          <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>{upperCase(filterLabel)}</AntText>
        );
        let value = filters[filter_key];
        if (["string", "boolean"].includes(typeof value)) {
          return (
            <div className={"global-filters-div-wrapper"} key={filter_key}>
              {label}
              <div>
                <Tag key={filterLabel} className="widget-filter_tags">
                  {getTransformedStringValue(value + "")}
                </Tag>
              </div>
            </div>
          );
        }
        if (isFilterDateTimeType(value)) {
          return (
            <div className={"global-filters-div-wrapper"} key={filter_key}>
              {label}
              <div>
                <Tag key={filterLabel} className="widget-filter_tags">
                  {`${moment.unix(parseInt(value.$gt)).utc().format("MM-DD-YYYY")} `} -{" "}
                  {` ${moment.unix(parseInt(value.$lt)).utc().format("MM-DD-YYYY")}`}
                </Tag>
              </div>
            </div>
          );
        }
        if (isArray(value) && value.length) {
          return (
            <div className={"global-filters-div-wrapper"} key={filter_key}>
              {label}
              <div>
                <Tag key={filterLabel} className="widget-filter_tags">
                  {transformArrayData(value, filter_key)?.join(", ")}
                </Tag>
              </div>
            </div>
          );
        }
        return null;
      })}
    </>
  );
};

export default TableFiltersPreview;
