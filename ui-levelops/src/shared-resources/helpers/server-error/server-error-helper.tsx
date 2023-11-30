import React from "react";

import { ServerErrorSource, ERROR_LINE1, ERROR_LINE2, STAT_ERROR } from "./constant";

export const getServerErrorDesc = (source: ServerErrorSource) => {
  let desc = <></>;
  switch (source) {
    case ServerErrorSource.SERVER_PAGINATED_TABLE:
    case ServerErrorSource.WIDGET:
      desc = (
        <div>
          <p className="error-line-1">{ERROR_LINE1}</p>
          <p className="error-line-2">{ERROR_LINE2}</p>
        </div>
      );
      break;
    case ServerErrorSource.STAT_WIDGET:
      desc = <div className="stat-error-text">{STAT_ERROR}</div>;
    default:
      break;
  }

  return desc;
};
