import React, { useCallback, useEffect, useState, useMemo } from "react";
import { Tag } from "antd";
import { AntCol, AntRow } from "../../../../components";
import { CascadeRestapi } from "../../../../helpers";
import { setFilterState } from "../../helper";
import { FILTER_TYPE } from "../../../../../constants/filters";

interface CascadeFilterProps {
  filter: any;
  onChange: (v: any) => void;
  onClose: (field: string, selected: any, index: number) => void;
}

export const CascadeFilter: React.FC<CascadeFilterProps> = props => {
  const { filter } = props;
  const selected = filter.selected || [];

  const [value, setValue] = useState([]);

  const handleChange = useCallback(
    (option: any) => {
      setValue(option);
      props.onChange(option);
    },
    [props.onChange]
  );

  const handleRemove = useCallback(
    (index: number) => {
      let tags = value || [];
      tags.splice(index, 1);
      props.onClose(filter.field, value, index);
    },
    [value, props.onClose]
  );

  useEffect(() => {
    setFilterState(value, selected, setValue);
  }, [filter]);

  const gutter = useMemo(() => [10, 10], []);

  return (
    <AntCol className="gutter-row" span={filter.span ? filter.span : 24}>
      <h5>{filter.label === "" ? filter.uri : filter.label}</h5>
      <AntRow type={"flex"} align={"middle"} gutter={gutter}>
        <AntCol span={8}>
          <CascadeRestapi
            style={{ width: "40%" }}
            id={`cascade-${filter.id}`}
            placeholder={filter.label}
            uri={filter.uri}
            searchField={filter.searchField}
            defaultValue={filter.selected || null}
            onChange={handleChange}
            createOption={false}
            mode={filter.type === FILTER_TYPE.API_MULTI_SELECT ? "multiple" : "default"}
            childMethod={filter.childMethod || "values"}
            fetchChildren={filter.fetchChildren}
          />
        </AntCol>
        <AntCol span={12}>
          {value.map((tag: string, index: number) => {
            return (
              <Tag
                closable={true}
                onClose={(event: any) => {
                  event.preventDefault();
                  handleRemove(index);
                }}>
                {tag}
              </Tag>
            );
          })}
        </AntCol>
      </AntRow>
    </AntCol>
  );
};

export default React.memo(CascadeFilter);
