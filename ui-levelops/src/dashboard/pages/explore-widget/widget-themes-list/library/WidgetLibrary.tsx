import React from "react";

import "./WidgetLibrary.scss";
import WidgetActionBar from "../../action-bar/WidgetActionBar";

import WidgetLibraryCategoryList from "./WidgetLibraryCategoryList";

interface WidgetLibraryProps {}

const WidgetLibrary: React.FC<WidgetLibraryProps> = () => {
  return (
    <>
      <div className="lib-header">Library</div>

      <div className="widget-library-container h-100">
        <WidgetActionBar />
        <WidgetLibraryCategoryList />
      </div>
    </>
  );
};

export default React.memo(WidgetLibrary);
