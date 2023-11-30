import React, { useEffect, useState } from "react";
import { Button } from "antd";
import CodeEditorModal from "shared-resources/components/code-editor-modal/CodeEditorModal";
import CodeMirrorEditor from "shared-resources/components/code-editor-modal/CodeMirrorEditor.lazy";

const CodeEditorWrapper: React.FC = (props: any) => {
  const { btnType, onCancel, onOk, onChange, value, btnText } = props;
  const [codeEditorModal, setCodeEditorModal] = useState<boolean>(false);
  const [editorValue, setEditorValue] = useState("");

  useEffect(
    () => {
      setEditorValue(value);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [props.value]
  );

  const onModalOk = () => {
    onOk && onOk();
    setCodeEditorModal(false);
    onChange && onChange(editorValue);
  };

  const onModalCancel = () => {
    onCancel && onCancel();
    setEditorValue(value);
    setCodeEditorModal(false);
  };

  const onValueChange = (eValue: string) => {
    setEditorValue(eValue);
  };

  return (
    <>
      {props.forDryRun ? (
        <CodeMirrorEditor value={value} onChange={onChange} />
      ) : (
        <>
          <Button type={btnType || "default"} style={{ margin: "1rem 0" }} onClick={() => setCodeEditorModal(true)}>
            {btnText || "View/Edit Script"}
          </Button>
          {codeEditorModal && (
            <CodeEditorModal
              value={editorValue || ""}
              onChange={onValueChange}
              visible={codeEditorModal}
              onOk={onModalOk}
              closable={false}
              onCancel={onModalCancel}
            />
          )}
        </>
      )}
    </>
  );
};

export default CodeEditorWrapper;
