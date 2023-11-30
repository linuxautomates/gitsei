import React, { useMemo, useState } from "react";
import { MAX_LENGTH_TO_SHOW, SCMRevCollabUserHeaderActionText } from "./constant";

interface SCMReviewUserListHeaderActionProps {
  setSlicingFactor: any;
  totalLength: number;
  visualLength: number;
  prefix: string;
  hovering: boolean;
}

const SCMReviewUserListHeaderActionComponent: React.FC<SCMReviewUserListHeaderActionProps> = ({
  setSlicingFactor,
  totalLength,
  visualLength,
  prefix,
  hovering
}) => {
  const [showingAll, setshowingAll] = useState<boolean>(false);
  const [showingTop25, setshowingTop25] = useState<boolean>(false);

  const handleShowAll = () => {
    setshowingAll(true);
    setSlicingFactor(totalLength);
  };

  const handleShowTop25 = () => {
    setshowingTop25(true);
    setSlicingFactor(25);
  };

  const handleShowLess = () => {
    if (showingAll) {
      setshowingAll(false);
      setSlicingFactor(Math.min(totalLength, MAX_LENGTH_TO_SHOW));
    }
    if (showingTop25) {
      setshowingTop25(false);
      setSlicingFactor(10);
    }
  };

  const getActionText = useMemo(() => {
    if (!showingAll && visualLength === totalLength) {
      return "";
    }
    if (!hovering) {
      if (totalLength <= 25) {
        return showingAll ? (
          <span onClick={handleShowLess}>{SCMRevCollabUserHeaderActionText.SHOW_LESS}</span>
        ) : (
          <span onClick={handleShowAll}>{SCMRevCollabUserHeaderActionText.SHOW_ALL}</span>
        );
      } else {
        return showingAll ? (
          <span onClick={handleShowLess}>{SCMRevCollabUserHeaderActionText.SHOW_LESS}</span>
        ) : (
          <span>
            {showingTop25 ? (
              <span onClick={handleShowLess}>{SCMRevCollabUserHeaderActionText.SHOW_LESS}</span>
            ) : (
              <span onClick={handleShowTop25}>{SCMRevCollabUserHeaderActionText.SHOW_TOP_25}</span>
            )}
            {" | "}
            <span onClick={handleShowAll}>{SCMRevCollabUserHeaderActionText.ALL}</span>
          </span>
        );
      }
    }
    return "";
  }, [totalLength, visualLength, showingAll, showingTop25, handleShowAll, handleShowTop25, handleShowTop25]);

  return (
    <div className="scm-user-list-header">
      <span className="scm-user-list-header-prefix">{prefix}</span>
      <span>{`(${visualLength}/${totalLength}) `}</span>
      <span className={"action-text"}>{getActionText}</span>
    </div>
  );
};

export default SCMReviewUserListHeaderActionComponent;
