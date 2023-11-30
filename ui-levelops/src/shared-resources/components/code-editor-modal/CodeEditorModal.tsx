import { Modal } from "antd";
import React from "react";
import CodeMirrorEditor from "./CodeMirrorEditor.lazy";
import "./CodeEditorModal.scss";

interface CodeEditorModalProps {
  visible: boolean;
  value: string;
  onChange: (value: string) => void;
  onOk: () => void;
  onCancel: () => void;
  closable?: boolean;
}

const CodeEditorModal: React.FC<CodeEditorModalProps> = (props: CodeEditorModalProps) => {
  return (
    <Modal
      wrapClassName={"code-editor-modal"}
      title={"Code Editor"}
      visible={props.visible}
      onCancel={props.onCancel}
      closable={props.closable}
      onOk={props.onOk}
      destroyOnClose={true}
      maskClosable={false}>
      <CodeMirrorEditor value={props.value} onChange={props.onChange} />
    </Modal>
  );
};

export default CodeEditorModal;
