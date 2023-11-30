import React from "react";
import { JSHINT } from "jshint";
import { Controlled as CodeMirror } from "react-codemirror2";
import "../../../../node_modules/codemirror/addon/lint/javascript-lint.js";
import "../../../../node_modules/codemirror/addon/lint/lint.css";
import "../../../../node_modules/codemirror/addon/lint/lint.js";
import "../../../../node_modules/codemirror/lib/codemirror.css";
import "../../../../node_modules/codemirror/mode/javascript/javascript";
import "../../../../node_modules/codemirror/theme/material.css";
import "./CodeMirrorEditor.scss";

(window as any).JSHINT = JSHINT;

interface CodeMirrorEditorProps {
  value: string;
  onChange: (value: string) => void;
}

const CodeMirrorEditor: React.FC<CodeMirrorEditorProps> = (props: CodeMirrorEditorProps) => {
  return (
    <div style={{ width: "100%", margin: "1rem 0" }}>
      <CodeMirror
        value={props.value}
        // @ts-ignore
        style={{ height: "100%" }}
        options={{
          mode: "javascript",
          lineNumbers: true,
          theme: "material",
          lint: true,
          gutters: ["CodeMirror-lint-markers"]
        }}
        onBeforeChange={(editor, data, value) => props.onChange(value)}
      />
    </div>
  );
};

export default CodeMirrorEditor;
