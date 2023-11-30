import { Tag } from "antd";
import Loader from "components/Loader/Loader";
import { useTables } from "custom-hooks";
import { get, isArray, upperCase } from "lodash";
import moment from "moment";
import React, { useCallback } from "react";
import { AntText } from "shared-resources/components";
import { isFilterDateTimeType } from "utils/timeUtils";

interface LevelopsTableFiltersPreviewProps {
  filters: any;
  metaData: Record<string, any>;
}

const LevelopsTableFiltersPreview: React.FC<LevelopsTableFiltersPreviewProps> = props => {
  const { filters, metaData } = props;
  const tableId = get(metaData, ["tableId"], "");
  const [tableGetLoading, tableGetData] = useTables("get", tableId, true, undefined, [tableId]);

  const getFilterName = useCallback(
    (key: string) => {
      const columns = tableGetData?.columns ?? [];
      const _column = columns.find((col: any) => col.id === key);
      return _column?.title ? _column.title : "Column";
    },
    [tableGetData]
  );

  if (tableGetLoading) return <Loader />;

  return (
    <>
      {Object.keys(filters).map((filter_key: string) => {
        const filterLabel = getFilterName(filter_key);
        const label = (
          <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>{upperCase(filterLabel)}</AntText>
        );
        const value = filters[filter_key];
        if (typeof value === "string") {
          return (
            <div className={"global-filters-div-wrapper"} key={filter_key}>
              {label}
              <div>
                <Tag key={filterLabel} className="widget-filter_tags">
                  {value}
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
                  {value.join(", ")}
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

export default LevelopsTableFiltersPreview;
