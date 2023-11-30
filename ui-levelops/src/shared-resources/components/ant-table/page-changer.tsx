import { Select } from "antd";
import React from "react";
import { AntSelectComponent as AntSelect } from "../ant-select/ant-select.component";

export interface CustomPageChangerProps {
  pageSizeOptions: string[];
  pageSize: string;
  onShowSizeChange: (size: number) => void;
}
export const CustomPageChanger: React.FC<any> = (props: any) => {
  const onSizeChange = (page: string) => {
    if (page != props.pageSize) {
      props.onShowSizeChange(page);
    }
  };
  return (
    <div>
      <AntSelect onChange={onSizeChange} value={`${props.pageSize} / page`}>
        {props.pageSizeOptions.map((pageSize: string) => (
          <Select.Option key={pageSize} value={pageSize}>
            {`${pageSize} / page`}
          </Select.Option>
        ))}
      </AntSelect>
    </div>
  );
};

export default CustomPageChanger;
