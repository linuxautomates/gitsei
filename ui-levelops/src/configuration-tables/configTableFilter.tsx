import React, { useState } from "react";
import { Button, DatePicker, Icon, Input, Select } from "antd";
import "./ConfigurationTablesConfigureModal.scss";
import { ConfigureTableFilterModal } from "./index";
import moment from "moment";
import { valueToUtcUnixTime } from "../utils/dateUtils";

const { RangePicker } = DatePicker;
const { Option } = Select;

interface ConfigTableFilterProps {
  columns: Array<any>;
  rows: Array<any>;
  onFilterChange: (data: any) => void;
  onRemoveFilter: (dataIndex: string) => void;
  selectedIndexes: Array<any>;
  setSelectedIndexes: (indexes: Array<any>) => void;
  filtersData: any;
}

const ConfigTableFilter: React.FC<ConfigTableFilterProps> = props => {
  const [selectedValues, setSelectedValues] = useState({});

  const [filterModal, setFilterModal] = useState<boolean>(false);

  const setValue = (index: string, value: any) => {
    setSelectedValues({ ...selectedValues, [index]: value });
    props.onFilterChange({ dataIndex: index, value });
  };

  const removeFilter = (index: string) => {
    setSelectedValues({ ...selectedValues, [index]: undefined });
    props.setSelectedIndexes(props.selectedIndexes.filter(value => value !== index));
    props.onRemoveFilter(index);
  };

  const onFilterModalOk = (data: Array<any>) => {
    props.setSelectedIndexes([...props.selectedIndexes, ...data]);
    setFilterModal(false);
  };

  const renderFilter = (dataIndex: string) => {
    const col = props.columns.find(col => col.dataIndex === dataIndex);
    const filterValue = props.filtersData.find((filter: any) => filter.dataIndex === dataIndex)?.value;
    if (col) {
      const type = col.inputType;
      switch (type) {
        case "boolean":
          return (
            <Select
              showArrow={false}
              style={{ minWidth: "12rem" }}
              value={filterValue}
              onChange={(value: any) => setValue(dataIndex, value)}>
              <Option value={"False"}>False</Option>
              <Option value={"True"}>True</Option>
            </Select>
          );
        case "date": {
          let gt = undefined,
            lt = undefined;
          if (filterValue?.hasOwnProperty("$gt") && filterValue?.hasOwnProperty("$lt")) {
            gt = moment.unix(filterValue?.$gt).utc();
            lt = moment.unix(filterValue?.$lt).utc();
          }
          return (
            <RangePicker
              allowClear={false}
              style={{ minWidth: "12rem" }}
              format={"MM/DD/YYYY"}
              value={!!gt && !!lt ? [gt, lt] : undefined}
              onChange={(date, dateString) =>
                setValue(dataIndex, {
                  $gt: valueToUtcUnixTime(dateString?.[0]),
                  $lt: valueToUtcUnixTime(dateString?.[1])
                })
              }
            />
          );
        }
        default:
          return (
            <Input
              style={{ minWidth: "12rem" }}
              value={filterValue}
              onChange={e => setValue(dataIndex, e.target.value)}
            />
          );
      }
    } else return null;
  };

  return (
    <div className={"mb-30 config-table-filter"}>
      {props.columns
        .map((col, index: number) => {
          if (props.selectedIndexes.includes(col.dataIndex)) {
            return (
              <div className={"tag"} key={index}>
                <span className="tag-title">{col.title} : </span>
                {renderFilter(col.dataIndex)}
                <Icon type={"close"} style={{ marginLeft: "0.7rem" }} onClick={() => removeFilter(col.dataIndex)} />
              </div>
            );
          }
          return undefined;
        })
        .filter((item: any) => item !== undefined)}
      <Button style={{ width: "10rem", margin: "3px" }} onClick={() => setFilterModal(true)}>
        <Icon type={"plus"} /> Add Filter
      </Button>
      <ConfigureTableFilterModal
        mappedColsRows={props.columns}
        selectedIndexes={props.selectedIndexes}
        visible={filterModal}
        onOk={onFilterModalOk}
        onCancel={() => setFilterModal(false)}
      />
    </div>
  );
};

export default ConfigTableFilter;
