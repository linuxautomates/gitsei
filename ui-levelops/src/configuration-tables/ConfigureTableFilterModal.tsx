import React, { useEffect, useState } from "react";
import { Checkbox, Modal } from "antd";
import "./ConfigureTableFilterModal.scss";

interface ConfigureTableFilterModalProps {
  mappedColsRows: Array<any>;
  selectedIndexes: Array<any>;
  visible: boolean;
  onOk: (data: Array<any>) => void;
  onCancel: () => void;
}

const ConfigureTableFilterModal: React.FC<ConfigureTableFilterModalProps> = (props: ConfigureTableFilterModalProps) => {
  const [selectGroups, setSelectGroups] = useState<Array<any>>([]);

  useEffect(() => setSelectGroups(props.selectedIndexes), [props.selectedIndexes]);

  const getCheckBoxOptions = () => {
    return props.mappedColsRows.map((col: any, index) => {
      return {
        label: col.title,
        value: col.dataIndex,
        disabled: props.selectedIndexes.includes(col.dataIndex)
      };
    });
  };

  const onCancel = () => {
    setSelectGroups([]);
    props.onCancel();
  };

  const onOk = () => {
    setSelectGroups([]);
    props.onOk(selectGroups);
  };

  return (
    <Modal
      wrapClassName={"configure-table-filter-modal"}
      title={"Select Columns to filter"}
      visible={props.visible}
      onOk={onOk}
      onCancel={onCancel}>
      <Checkbox.Group options={getCheckBoxOptions()} value={selectGroups} onChange={data => setSelectGroups(data)} />
    </Modal>
  );
};

export default ConfigureTableFilterModal;
