import React, { useEffect, useMemo, useRef, useState } from "react";
import cx from "classnames";
import { Button } from "antd";
import sanitizeHtml from "sanitize-html";
import { useDemoDashboardDataId } from "custom-hooks/useDemoDashboardDataKey";
import { get } from "lodash";
import "../../../../../configurable-dashboard/components/widget-preview/custom-preview/dashboard-notes-preview/dashboard-notes-preview.style.scss";

interface DemoDashboardNotesPreviewProps {
  dashboardId: string;
  widgetId: string;
}

const DEFAULT_HEIGHT = 230;

const DemoDashboardNotesPreviewComponent: React.FC<DemoDashboardNotesPreviewProps> = ({ dashboardId, widgetId }) => {
  const demoDatakey: string = useDemoDashboardDataId(widgetId) as string;

  const widgetRef = useRef();
  let descriptionString = get(widgetId, ["data", demoDatakey, "data", "description"], null);

  const [showMore, setShowMore] = useState<boolean>(false);
  const [hasOverflow, setHasOverflow] = useState<boolean>(false);
  const [description, setDescription] = useState<string>("");
  // saving max height for animation
  const [maxHeight, setMaxHeight] = useState<string>("auto");

  useEffect(() => {
    descriptionString?.length && setDescription(descriptionString);
  }, [descriptionString]);

  const toggleShowMore = () => {
    setShowMore(!showMore);
  };

  const contentRef = (_ref: any) => {
    if (maxHeight === "auto" && _ref?.clientHeight > DEFAULT_HEIGHT + 10) {
      setMaxHeight(`${_ref?.clientHeight}px`);
      setHasOverflow(true);
    }
  };

  useEffect(() => {
    maxHeight !== "auto" && setMaxHeight("auto");
  }, [description]);

  const content = useMemo(() => {
    const _height = hasOverflow ? (showMore ? maxHeight : `${DEFAULT_HEIGHT}px`) : maxHeight;
    // sanitizing data to prevent xss
    const sanitizeWidgetDescription = sanitizeHtml(description);
    return (
      <div
        className="html-string-content"
        ref={contentRef}
        style={{ height: _height }}
        dangerouslySetInnerHTML={{ __html: sanitizeWidgetDescription }}
      />
    );
  }, [description]);

  return (
    <>
      <div ref={widgetRef as any} className={cx("dashboard-notes-preview dashboard-notes")}>
        {
          <div className="flex">
            <div className="dashboard-notes-preview__content">
              {content}
              {hasOverflow && (
                <Button
                  className="dashboard-notes-preview__content--content-view-button"
                  onClick={toggleShowMore}
                  icon={showMore ? "up" : "down"}
                  type="link">
                  {showMore ? "Show Less" : "Show More"}
                </Button>
              )}
            </div>
          </div>
        }
      </div>
    </>
  );
};

export default React.memo(DemoDashboardNotesPreviewComponent);
