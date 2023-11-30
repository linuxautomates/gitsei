import React, { useState } from "react";
import { Modal, Radio } from "antd";
import { RadioChangeEvent } from "antd/lib/radio";
import * as FileSaver from "file-saver";
import StringsEn from "../../../locales/StringsEn";
interface ExportModalComponentProps {
  chart: any;
  visible: boolean;
  onOK: () => void;
}

export const ExportModalComponent: React.FC<ExportModalComponentProps> = (props: ExportModalComponentProps) => {
  const { chart, onOK, visible } = props;
  const [exportValue, setExportValue] = useState(0);

  const updateExportValue = (e: RadioChangeEvent) => {
    setExportValue(e.target.value);
  };

  const exportPropel = () => {
    let jsonData = undefined;
    switch (exportValue) {
      case 0:
        jsonData = chart.empty_post_data;
        break;
      case 1:
        jsonData = chart.static_post_data;
        break;
      case 2:
        jsonData = chart.post_data_no_passwords;
        break;
      default:
        jsonData = chart.empty_post_data;
        break;
    }
    const jsonString = JSON.stringify(jsonData, null, 4);
    let file = new File([jsonString], `${chart.name}.json`, { type: "text/plain;charset=utf-8" });
    FileSaver.saveAs(file);
    onOK();
  };

  const radioStyle = {
    display: "block",
    height: "30px",
    lineHeight: "30px"
  };

  return (
    <Modal title={StringsEn.propelsExport} visible={visible} okText={"Export"} onOk={exportPropel} onCancel={onOK}>
      <Radio.Group onChange={updateExportValue} value={exportValue}>
        <Radio value={0} style={radioStyle}>
          Don't Include any Configurations
        </Radio>
        <Radio value={1} style={radioStyle}>
          Export Static Configurations Only
        </Radio>
        <Radio value={2} style={radioStyle}>
          Export All Configurations
        </Radio>
      </Radio.Group>
    </Modal>
  );
};
