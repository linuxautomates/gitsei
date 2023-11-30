import React, { useEffect, useState } from "react";
import { Button, Col, Row } from "antd";
import "./text-annotate.container.scss";
import { AntButton, AntText } from "shared-resources/components";
import { setMarkBackground } from "./text-annotate.help";
import reactStringReplace from "react-string-replace";

interface TextAnnotateContainerProps {
  text: string;
  annotations: Array<any>;
  setAnnotationList: (newList: any[]) => void;
  handleAnnotateChange: (data: any) => void;
}

const HIT_COLOR = "#A9C6E3";
const BASE_COLOR = "#bcdcfd";

export const TextAnnotateContainer: React.FC<TextAnnotateContainerProps> = (props: TextAnnotateContainerProps) => {
  const [position, setPosition] = useState({ x: undefined, y: undefined });
  const [showAnnotate, setShowAnnotateOptions] = useState(false);
  const [currentSelect, setCurrentSelect] = useState<string[]>([]);
  const [selectionRef, setSelectionRef] = useState<any>(undefined);
  const [markIds, setMarkIds] = useState<any[]>([]);
  const [currentMarker, setCurrentMarker] = useState(0);
  const [markedText, setMarkedText] = useState<any[]>([]);
  const [markFirst, setMarkFirst] = useState(0);

  useEffect(() => {
    if (markedText.length > 0) {
      const firstIndex = markedText.findIndex((item: any) => typeof item !== "string" && item.hasOwnProperty("props"));
      if (firstIndex !== -1) {
        const item = markedText[firstIndex];
        setMarkedText((text: any[]) => [
          ...text.slice(0, firstIndex),
          setMarkBackground(item, HIT_COLOR),
          ...text.slice(firstIndex + 1)
        ]);
        const el = document.getElementById(item.props.id);
        if (el) {
          el.scrollIntoView({ behavior: "smooth" });
        }
      }
    }
  }, [markFirst]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    let markedText = props.text;
    let idSequence = 0;

    for (const ann of props.annotations) {
      let hasWrappingBraces = false;

      if (!ann) {
        continue;
      }

      // Making sure it doesn't crash on undefined.
      if (ann && !!ann.length) {
        hasWrappingBraces = ann[0] === "(" && ann[ann.length - 1] === ")";
      }

      let sanitized = ann.replace(/^(\(\?[\g\i]*\))/g, ""); // eslint-disable-line
      sanitized = sanitized.replace("?", "\\?");
      let regex = undefined;
      try {
        regex = new RegExp(hasWrappingBraces ? sanitized : `(${sanitized})`, "ig");
      } catch (e) {
        console.log("caught error", e);
        regex = sanitized;
      }

      // @ts-ignore
      markedText = reactStringReplace(
        markedText,
        regex,
        // eslint-disable-next-line
        (match: React.ReactNode, i: any, offset: any) => {
          // eslint-disable-next-line
          const id = `annotation_marker_id_${idSequence++}`;
          return (
            <mark style={{ backgroundColor: "" }} id={id} key={id}>
              {match}
            </mark>
          );
        }
      );
    }

    if (typeof markedText !== "string") {
      const ids: any[] = [];
      (markedText as any).forEach((item: any) => {
        if (typeof item === "object" && item.hasOwnProperty("props")) {
          ids.push(item.props.id);
        }
      });
      if (ids.length !== markIds.length) {
        setMarkIds(ids);
      }
      setMarkedText(markedText);
      setCurrentMarker(0);
      setMarkFirst((num: number) => num + 1);
      return;
    }

    setMarkedText([markedText]);
    setCurrentMarker(0);
    setMarkFirst((num: number) => num + 1);
  }, [props.annotations]); // eslint-disable-line react-hooks/exhaustive-deps

  const onMouseUpHandler = (e: React.MouseEvent) => {
    // TODO if it has already been annotated, dont annotate again
    e.preventDefault();
    const selectionObj = window.getSelection && window.getSelection();
    if (selectionObj) {
      const selection = selectionObj.toString();
      if (!selection || selection === "") {
        resetSelections();
        return;
      }
      const selectionRange = selectionObj.getRangeAt(0);

      // @ts-ignore
      const { x, y, width } = selectionRange.getBoundingClientRect();
      if (width && width > 1 && selection.length > 0) {
        // @ts-ignore
        setPosition({ x: x + width / 2, y: y - 10 });
        setShowAnnotateOptions(true);
        setCurrentSelect([selection]);
        // @ts-ignore
        setSelectionRef(selectionObj);
      } else {
        console.log("hiding popover");
        resetSelections();
      }
    }
  };

  const resetSelections = () => {
    if (!showAnnotate && !currentSelect.length) {
      return;
    }
    setShowAnnotateOptions(false);
    setCurrentSelect([]);
    setPosition({ x: undefined, y: undefined });

    if (selectionRef && selectionRef.empty) {
      // Chrome
      selectionRef.empty();
    } else if (selectionRef.removeAllRanges) {
      // Firefox
      selectionRef.removeAllRanges();
    }
    setSelectionRef(undefined);
  };

  const annotateContent = () => {
    const handleAnnotate = (e: any, type: string) => {
      props.handleAnnotateChange({ data: currentSelect, type });
      e.preventDefault();
      resetSelections();
    };

    return (
      <Row gutter={[10, 10]}>
        <Col span={24}>
          <Button
            type={"link"}
            onClick={e => handleAnnotate(e, "edit")}
            style={{ color: "#FFF", marginRight: "5px" }}
            icon={"edit"}
          />
          <Button
            type={"link"}
            onClick={e => handleAnnotate(e, "add")}
            style={{ color: "#FFF" }}
            icon={"plus-circle"}
          />
        </Col>
      </Row>
    );
  };

  const markerChanged = (e: any, type: string) => {
    e.preventDefault();
    const id = type === "up" ? currentMarker - 1 : currentMarker + 1;
    if (id >= 0 && id < markIds.length) {
      const currentIndex = markedText.findIndex(
        (item: any) =>
          typeof item === "object" && item.hasOwnProperty("props") && item.props.id === markIds[currentMarker]
      );
      const newIndex = markedText.findIndex(
        (item: any) => typeof item === "object" && item.hasOwnProperty("props") && item.props.id === markIds[id]
      );

      if (currentIndex !== -1 && newIndex !== -1) {
        const currentItem = markedText[currentIndex];
        const newItem = markedText[newIndex];
        let updatedMarkedText = [];
        if (type === "up") {
          updatedMarkedText = [
            ...markedText.slice(0, newIndex),
            setMarkBackground(newItem, HIT_COLOR),
            ...markedText.slice(newIndex + 1, currentIndex),
            setMarkBackground(currentItem, BASE_COLOR),
            ...markedText.slice(currentIndex + 1)
          ];
        } else {
          updatedMarkedText = [
            ...markedText.slice(0, currentIndex),
            setMarkBackground(currentItem, BASE_COLOR),
            ...markedText.slice(currentIndex + 1, newIndex),
            setMarkBackground(newItem, HIT_COLOR),
            ...markedText.slice(newIndex + 1)
          ];
        }

        setMarkedText(updatedMarkedText);
      }

      const el = document.getElementById(markIds[id]);
      el && el.scrollIntoView({ behavior: "smooth" });
      setCurrentMarker(id);
    }
  };

  return (
    <div>
      {showAnnotate && (
        <div
          className={"h-popover"}
          style={{ position: "fixed", left: `${position.x}px`, top: `${position.y}px` }}
          role="presentation"
          key="showAnnotatePopup"
          onMouseDown={e => e.preventDefault()}>
          {annotateContent()}
        </div>
      )}
      <span
        style={{
          whiteSpace: "pre-line",
          wordBreak: "break-word",
          lineHeight: "200%",
          display: "block",
          paddingRight: "3rem"
        }}
        onMouseDown={resetSelections}
        onMouseUp={onMouseUpHandler}>
        {props.annotations.length > 0 && markedText.length > 3 && (
          <div style={{ position: "absolute", right: "16px", top: "5px" }}>
            <AntText style={{ display: "block", fontSize: "16px", marginBottom: "5px", textAlign: "center" }}>
              {`${currentMarker + 1}/${markIds.length}`}
            </AntText>
            <AntButton
              icon="up-circle"
              style={{ marginRight: "10px" }}
              // disabled={currentMarker <= 0}
              onClick={(e: any) => markerChanged(e, "up")}
            />
            <AntButton
              icon="down-circle"
              // disabled={currentMarker >= markIds.length - 1}
              onClick={(e: any) => markerChanged(e, "down")}
            />
          </div>
        )}
        {markedText}
      </span>
    </div>
  );
};
