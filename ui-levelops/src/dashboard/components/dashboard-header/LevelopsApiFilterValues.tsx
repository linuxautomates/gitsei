import React, { useMemo } from "react";
import { v1 as uuid } from "uuid";
import { useApi } from "../../../custom-hooks";
import { Tag } from "antd";

interface LevelopsApiFilterValuesProps {
  filterValues: string[];
  uri: string;
  reportType: string;
  labelField: string;
}

const LevelopsApiFilterValues: React.FC<LevelopsApiFilterValuesProps> = ({
  filterValues,
  uri,
  reportType,
  labelField
}) => {
  const api = useMemo(
    () => ({
      id: `${uri}_${uuid()}`,
      apiName: uri,
      apiMethod: "list",
      filters: {
        page_size: 10000
      },
      reportType
    }),
    [uri]
  );

  const [loading, apiData, apisMetaData] = useApi([api], []);
  const getFilterValueName: any = (value: string) => {
    const allData = Object.values(apiData || {});
    if (allData.length) {
      return (allData[0] as any).find((data: any) => data?.id === value);
    }
  };
  if (loading) {
    return <></>;
  }
  return (
    <>
      {filterValues.map(value => (
        <div>
          <Tag key={value} className="widget-filter_tags">
            {getFilterValueName(value) ? getFilterValueName(value)[labelField] : value}
          </Tag>
        </div>
      ))}
    </>
  );
};

export default LevelopsApiFilterValues;
